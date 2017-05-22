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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private boolean hideBuildHistory;

    private boolean hideJobs;

    private boolean hideBuildQueue;

    private boolean hideNodes;

    private String statusButtonSize;

    private String layoutHeightRatio;

    private String filterRegex;

    @DataBoundConstructor
    public MissionControlView(String name) {
        super(name);
        this.fontSize = 16;
        this.buildQueueSize = 10;
        this.buildHistorySize = 16;
        this.useCondensedTables = false;
        this.hideBuildHistory = false;
        this.hideJobs = false;
        this.hideBuildQueue = false;
        this.hideNodes = false;
        this.statusButtonSize = "";
        this.layoutHeightRatio = "6040";
        this.filterRegex = null;
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

    public String getFilterRegex() {
        return filterRegex;
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
        this.hideBuildHistory = json.getBoolean("hideBuildHistory");
        this.hideJobs = json.getBoolean("hideJobs");
        this.hideBuildQueue = json.getBoolean("hideBuildQueue");
        this.hideNodes = json.getBoolean("hideNodes");
        if (json.get("useRegexFilter") != null ) {
            String regexToTest = req.getParameter("filterRegex");
            try {
                Pattern.compile(regexToTest);
                this.filterRegex = regexToTest;
            } catch (PatternSyntaxException x) {
                Logger.getLogger(ListView.class.getName()).log(Level.WARNING, "Regex filter expression is invalid", x);
            }
        } else {
            this.filterRegex = null;
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
        Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;

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

        Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;
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

            statuses.add(new JobStatus(j.getFullName(), status));
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
}
