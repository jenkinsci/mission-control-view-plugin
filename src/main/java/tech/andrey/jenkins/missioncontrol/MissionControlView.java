package tech.andrey.jenkins.missioncontrol;

import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@SuppressWarnings("unused")
@ExportedBean
public class MissionControlView extends View {
    private transient int getBuildsLimit;

    private int fontSize;

    private int buildQueueSize;

    private int buildHistorySize;

    private boolean useCondensedTables;

    private boolean filterByFailures;

    private boolean hideBuildHistory;

    private boolean hideJobs;

    private boolean hideBuildQueue;

    private boolean hideNodes;

    private String statusButtonSize;

    private String layoutHeightRatio;

    private String filterBuildHistory;

    private String filterJobStatuses;

    @DataBoundConstructor
    public MissionControlView(String name) {
        super(name);
        this.fontSize = 16;
        this.buildQueueSize = 10;
        this.buildHistorySize = 16;
        this.useCondensedTables = false;
        this.filterByFailures = false;
        this.hideBuildHistory = false;
        this.hideJobs = false;
        this.hideBuildQueue = false;
        this.hideNodes = false;
        this.statusButtonSize = "";
        this.layoutHeightRatio = "6040";
        this.filterBuildHistory = null;
        this.filterJobStatuses = null;
    }

    protected Object readResolve() {
        if (getBuildsLimit == 0)
            getBuildsLimit = 250;

        if (fontSize == 0)
            fontSize = 16;

        if (buildHistorySize == 0)
            buildHistorySize = 16;

        if (buildQueueSize == 0)
            buildQueueSize = 10;

        if (statusButtonSize == null)
            statusButtonSize = "";

        if (layoutHeightRatio == null)
            layoutHeightRatio = "6040";

        return this;
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return new ArrayList<TopLevelItem>();
    }

    public int getFontSize() {
        return fontSize;
    }

    public int getBuildHistorySize() {
        return buildHistorySize;
    }

    public int getBuildQueueSize() {
        return buildQueueSize;
    }

    public boolean isUseCondensedTables() {
        return useCondensedTables;
    }

    public boolean isFilterByFailures() {
        return filterByFailures;
    }

    public String getTableStyle() {
        return useCondensedTables ? "table-condensed" : "";
    }

    public String getBuildHistoryHeight() {
        if (hideBuildHistory)
            return "0";
        if (hideBuildQueue)
            return "100";
        return getTopHalfHeight();
    }

    public boolean isHideBuildHistory() {
        return hideBuildHistory;
    }

    public String getDisplayBuildHistory() {
        return hideBuildHistory ? "display: None" : "";
    }

    public String getJobsHeight() {
        if (hideJobs)
            return "0";
        if (hideNodes)
            return "100";
        return getTopHalfHeight();
    }

    public boolean isHideJobs() {
        return hideJobs;
    }

    public String getDisplayJobs() {
        return hideJobs ? "display: None" : "";
    }

    public String getBuildQueueHeight() {
        if (hideBuildQueue)
            return "0";
        if (hideBuildHistory)
            return "100";
        return getBottomHalfHeight();
    }

    public boolean isHideBuildQueue() {
        return hideBuildQueue;
    }

    public String getDisplayBuildQueue() {
        return hideBuildQueue ? "display: None" : "";
    }

    public String getNodesHeight() {
        if (hideNodes)
            return "0";
        if (hideJobs)
            return "100";
        return getBottomHalfHeight();
    }

    public boolean isHideNodes() {
        return hideNodes;
    }

    public String getDisplayNodes() {
        return hideNodes ? "display: None" : "";
    }

    public String getStatusButtonSize() {
        return statusButtonSize;
    }

    public String getLayoutHeightRatio() {
        return layoutHeightRatio;
    }

    public String getFilterBuildHistory() {
        return filterBuildHistory;
    }

    public String getFilterJobStatuses() {
        return filterJobStatuses;
    }

    private String getTopHalfHeight() {
        return layoutHeightRatio.substring(0, 2);
    }

    private String getBottomHalfHeight() {
        return layoutHeightRatio.substring(2, 4);
    }

    @Override
    protected void submit(StaplerRequest req) throws ServletException, IOException {
        JSONObject json = req.getSubmittedForm();
        this.fontSize = json.getInt("fontSize");
        this.buildHistorySize = json.getInt("buildHistorySize");
        this.buildQueueSize = json.getInt("buildQueueSize");
        this.useCondensedTables = json.getBoolean("useCondensedTables");
        this.filterByFailures = json.getBoolean("filterByFailures");
        this.hideBuildHistory = json.getBoolean("hideBuildHistory");
        this.hideJobs = json.getBoolean("hideJobs");
        this.hideBuildQueue = json.getBoolean("hideBuildQueue");
        this.hideNodes = json.getBoolean("hideNodes");
        if (json.get("useRegexFilterBuildHistory") != null ) {
            String buildHistoryRegexToTest = req.getParameter("filterBuildHistory");
            try {
                Pattern.compile(buildHistoryRegexToTest);
                this.filterBuildHistory = buildHistoryRegexToTest;
            } catch (PatternSyntaxException x) {
                Logger.getLogger(ListView.class.getName()).log(Level.WARNING, "Regex filter expression is invalid", x);
            }
        } else {
            this.filterBuildHistory = null;
        }
        if (json.get("useRegexFilterJobStatuses") != null ) {
            String jobStatusesRegexToTest = req.getParameter("filterJobStatuses");
            try {
                Pattern.compile(jobStatusesRegexToTest);
                this.filterJobStatuses = jobStatusesRegexToTest;
            } catch (PatternSyntaxException x) {
                Logger.getLogger(ListView.class.getName()).log(Level.WARNING, "Regex filter expression is invalid", x);
            }
        } else {
            this.filterJobStatuses = null;
        }
        this.statusButtonSize = json.getString("statusButtonSize");
        this.layoutHeightRatio = json.getString("layoutHeightRatio");
        save();
    }

    @Override
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(TopLevelItem item) {
        return false;
    }

    @Override
    public boolean hasPermission(final Permission p) { return true; }

    /**
     * This descriptor class is required to configure the View Page
     */
    @Extension
    public static final class DescriptorImpl extends ViewDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.MissionControlView_DisplayName();
        }
    }

    public Api getApi() {
        return new Api(this);
    }

    @Exported(name="builds")
    public Collection<Build> getBuildHistory() {
        ArrayList<Build> l = new ArrayList<Build>();
        Jenkins instance = Jenkins.getInstance();
        if (instance == null)
            return l;

        List<Job> jobs = instance.getAllItems(Job.class);
        RunList builds = new RunList(jobs).limit(getBuildsLimit);
        Pattern r = filterBuildHistory != null ? Pattern.compile(filterBuildHistory) : null;

        for (Object b : builds) {
            Run build = (Run)b;
            Job job = build.getParent();

            // Skip Maven modules. They are part of parent Maven project
            if (job.getClass().getName().equals("hudson.maven.MavenModule"))
                continue;

            // If filtering is enabled, skip jobs not matching the filter
            if (r != null && !r.matcher(job.getFullName()).find())
                continue;

            Result result = build.getResult();
            l.add(new Build(job.getName(),
                    build.getFullDisplayName(),
                    build.getNumber(),
                    build.getStartTimeInMillis(),
                    build.getDuration(),
                    result == null ? "BUILDING" : result.toString()));
        }

        return l;
    }

    @ExportedBean(defaultVisibility = 999)
    public static class Build {
        @Exported
        public String jobName;
        @Exported
        public String buildName;
        @Exported
        public int number;
        @Exported
        public long startTime;
        @Exported
        public long duration;
        @Exported
        public String result;

        public Build(String jobName, String buildName, int number, long startTime, long duration, String result) {
            this.jobName = jobName;
            this.buildName = buildName;
            this.number = number;
            this.startTime = startTime;
            this.duration = duration;
            this.result = result;
        }
    }

    @Exported(name="allJobsStatuses")
    public Collection<JobStatus> getAllJobsStatuses() {
        String status;
        ArrayList<JobStatus> statuses = new ArrayList<JobStatus>();

        Jenkins instance = Jenkins.getInstance();
        if (instance == null)
            return statuses;

        Pattern r = filterJobStatuses != null ? Pattern.compile(filterJobStatuses) : null;
        List<Job> jobs = instance.getAllItems(Job.class);

        for (Job j : jobs) {
            // Skip matrix configuration sub-jobs and Maven modules
            if (j.getClass().getName().equals("hudson.matrix.MatrixConfiguration")
                    || j.getClass().getName().equals("hudson.maven.MavenModule"))
                continue;

            // If filtering is enabled, skip jobs not matching the filter
            if (r != null && !r.matcher(j.getName()).find())
                continue;

            if (j.isBuilding()) {
                status = "BUILDING";
            } else if (!j.isBuildable()) {
                status = "DISABLED";
            } else {
                Run lb = j.getLastBuild();
                if (lb == null) {
                    status = "NOTBUILT";
                } else {
                    Result res = lb.getResult();
                    status = res == null ? "UNKNOWN" : res.toString();
                }
            }

            // Decode pipeline branch names
            String fullName = j.getFullName();
            try {
                fullName = URLDecoder.decode(j.getFullName(), "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            statuses.add(new JobStatus(fullName, status));
        }

        if (filterByFailures) {
            Collections.sort(statuses, new StatusComparator());
        }

        return statuses;
    }

    @ExportedBean(defaultVisibility = 999)
    public static class JobStatus {
        @Exported
        public String jobName;
        @Exported
        public String status;

        public JobStatus(String jobName, String status) {
            this.jobName = jobName;
            this.status = status;
        }
    }

    static class StatusComparator implements Serializable, Comparator<JobStatus> {
        public static Map<String, Integer> statuses = new HashMap<String, Integer>();
        static {
            statuses.put("BUILDING", 1);
            statuses.put("FAILURE", 2);
            statuses.put("UNSTABLE", 3);
            statuses.put("ABORTED", 4);
            statuses.put("SUCCESS", 5);
            statuses.put("NOTBUILT", 6);
            statuses.put("DISABLED", 7);
        }

        public int compare(JobStatus o1, JobStatus o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }

            int o1level, o2level;

            o1level = statuses.get(o1.status.toUpperCase());
            o2level = statuses.get(o2.status.toUpperCase());
            if (o1level < o2level) {
                return -1;
            }
            if (o1level > o2level) {
                return +1;
            }
            else {
                return 0;
            }
        }
    };

}
