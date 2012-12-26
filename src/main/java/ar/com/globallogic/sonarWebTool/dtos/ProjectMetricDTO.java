package ar.com.globallogic.sonarWebTool.dtos;

public class ProjectMetricDTO {

	private String date;
	private String projectName;
	private String metricName;
	private String metricValue;
	
	public ProjectMetricDTO(String date, String projectName, String metricName, String metricValue) {
		super();
		this.date = date;
		this.projectName = projectName;
		this.metricName = metricName;
		this.metricValue = metricValue;
	}
	
	public String getDate() {
		return date;
	}
	public String getprojectName() {
		return projectName;
	}
	public String getMetricName() {
		return metricName;
	}
	public String getMetricValue() {
		return metricValue;
	}
	
	
}
