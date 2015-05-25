package nz.ac.auckland.researchoutputs.rtc;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.eperson.EPerson;

import org.apache.log4j.Logger;

/**
 * Checks bitstream policies for files requested by
 * Symplectic Elements, and rejects the request if
 * context user does not have READ access
 */
public class RepositoryToolsRestrictionFilter implements Filter {

    public static final Logger log = Logger.getLogger(RepositoryToolsRestrictionFilter.class);

    // Configuration defaults
    private boolean enabled = true;
    private static int segment;

    private String defaultUser = "digital.development@auckland.ac.nz";
    private int defaultSegment = 3;

    private Context c;

    public RepositoryToolsRestrictionFilter() {
        // stub
    }

    public void destroy() {
        // stub
    }

    /**
     * Read servlet context config, disable if enable is false
     *
     * "enable" can be true or false and determines whether to enable the filter. defaults to true.
     * "testuser" should be an email address for a DSpace account to use in authorisation context.
     *            This means it should have the *minimum* necessary privileges to see items which
     *            you will allow via /rt4ds/repository/file/*
     *            I recommend a test user in no groups other than the special Anonymous group, but
     *            it could also be added to other groups for different handling.
     *            Default is digital.development@auckland.ac.nz
     *
     * "segment" should reflect the index of the URI path segment in which the bitstream ID can be found.
     *           eg. if the full URL is http://myrepository.com/rt4ds/repository/file/12345/myfile.pdf then
     *           the segment indices are 0=rt4ds, 1=repository, 2=file, 3=12345, 4=myfile.pdf
     *           The default is '3' as this reflects the most common setup.
     *
     * @param config
     * @throws ServletException
     */
    public void init(FilterConfig config) throws ServletException {

        log.info("RTC Filter: Initialising");

        String restrict = config.getServletContext().getInitParameter("RepositoryToolsAuthFilter.enable");
        String testuser = config.getServletContext().getInitParameter("RepositoryToolsAuthFilter.testuser");

        if("false".equalsIgnoreCase(restrict))
            enabled = false;

        if(null == testuser || "".equals(testuser)) {
            testuser = defaultUser;
        }

        try {
            segment = Integer.parseInt(config.getServletContext().getInitParameter("RepositoryToolsAuthFilter.segment"));
        } catch(NumberFormatException e) {
            segment = defaultSegment;
        }

        // should this actually be in the doFilter? could be keeping db statments open too long
        if(enabled) {
            try {
                c = new Context();
                c.setCurrentUser(EPerson.findByEmail(c,testuser));
            } catch(Exception e) {
                log.info("RTC Filter: Could not initialise DSpace context as " + testuser);
                throw new ServletException(e);
            }
        }
    }

    /**
     * Do the filtering, checking if this context can READ the DSO in question.
     * @param req
     * @param res
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {

        if(enabled) {
            try {
                // Get bitstream ID from request URL
                String uri = ((HttpServletRequest)req).getRequestURI();
                String bitstreamId = getBitstreamId(uri);

                log.info("RTC Filter: Incoming request for bitstream ID " + bitstreamId + " from user " + c.getCurrentUser().getEmail() + ", IP " + req.getRemoteAddr());

                // Check if it's able to be read with current context (as "testuser", see init)
                Bitstream b = Bitstream.find(c,Integer.parseInt(bitstreamId));

                // Here, we could do more checks like "is it a thesis? does it have current embargo terms? etc."
                // [...]

                boolean authorized = AuthorizeManager.authorizeActionBoolean(c, b, Constants.READ);

                if(!authorized) {
                    // Bad. No looky. Send 403.
                    log.info("RTC Filter: "+ c.getCurrentUser().getEmail() +" not authorised to READ bitstream ID " + bitstreamId +  ", sending 403 response");
                    ((HttpServletResponse)res).sendError(403);
                }
                else {
                    log.info("RTC Filter: "+ c.getCurrentUser().getEmail() +" is authorised to READ bitstream ID " + bitstreamId +  ", carrying on filter chain");
                }

            } catch(Exception e) {
                throw new ServletException(e);
            }
        }

        // Guess we're OK. Carry on!
        chain.doFilter(req, res);
    }

    public static String getBitstreamId(final String url) {
        String[] matches = url.split("/");
        return matches[segment];
    }

}
