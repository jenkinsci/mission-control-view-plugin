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

@SuppressWarnings("unused")
@ExportedBean
public class MissionControlView extends View {
    private int getBuildsLimit;

    private String viewName;

    private int fontSize;

    private boolean useCondensedTables;

    private String statusButtonSize;

    private String layoutHeightRatio;

    @DataBoundConstructor
    public MissionControlView(String name, String viewName) {
        super(name);
        this.viewName = viewName;
        this.fontSize = 16;
        this.getBuildsLimit = 50;
        this.useCondensedTables = false;
        this.statusButtonSize = "default";
        this.layoutHeightRatio = "6040";
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return new ArrayList<TopLevelItem>();
    }

    public int getFontSize() {
        return fontSize;
    }

    public boolean isUseCondensedTables() {
        return useCondensedTables;
    }

    public String getTableStyle() {
        return useCondensedTables ? "table-condensed" : "";
    }

    public String getStatusButtonSize() {
        return statusButtonSize;
    }

    public String getLayoutHeightRatio() {
        return layoutHeightRatio;
    }

    public String getTopHalfHeight() {
        return layoutHeightRatio.substring(0, 2);
    }

    public String getBottomHalfHeight() {
        return layoutHeightRatio.substring(2, 4);
    }

    @Override
    protected void submit(StaplerRequest req) throws ServletException, IOException {
        JSONObject json = req.getSubmittedForm();
        this.fontSize = json.getInt("fontSize");
        this.useCondensedTables = json.getBoolean("useCondensedTables");
        this.statusButtonSize = json.getString("statusButtonSize");
        this.layoutHeightRatio = json.getString("layoutHeightRatio");
        save();
    }

    @Override
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        return Jenkins.getInstance().doCreateItem(req, rsp);
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
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
        RunList builds = new RunList(jobs).limit(this.getBuildsLimit);
        ArrayList<Build> l = new ArrayList<Build>();
        for (Object b : builds) {
            Run build = (Run)b;
            Job job = build.getParent();
            // Skip Maven modules. They are part of parent Maven project
            if (job.getClass().getName().equals("hudson.maven.MavenModule"))
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
    public class Build {
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
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
        ArrayList<JobStatus> statuses = new ArrayList<JobStatus>();
        String status, name;

        for (Job j : jobs) {
            // Skip matrix configuration sub-jobs and Maven modules
            if (j.getClass().getName().equals("hudson.matrix.MatrixConfiguration")
                    || j.getClass().getName().equals("hudson.maven.MavenModule"))
                continue;

            if (j.isBuilding()) {
                status = "BUILDING";
            } else {
                Run lb = j.getLastBuild();
                if (lb == null) {
                    status = "NOTBUILT";
                } else {
                    status = lb.getResult().toString();
                }
            }

            ItemGroup parent = j.getParent();
            if (parent != null && parent.getClass().getName().equals("com.cloudbees.hudson.plugins.folder.Folder")) {
                name = parent.getFullName() + " / " + j.getName();
            } else {
                name = j.getName();
            }

            statuses.add(new JobStatus(name, status));
        }

        return statuses;
    }

    @ExportedBean(defaultVisibility = 999)
    public class JobStatus {
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