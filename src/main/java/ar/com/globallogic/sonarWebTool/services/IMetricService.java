package ar.com.globallogic.sonarWebTool.services;

import java.util.List;

import ar.com.globallogic.sonarWebTool.dtos.ProjectMetricDTO;

public interface IMetricService {

	List<ProjectMetricDTO> getMetricInfo(String url,String user, String password,String projectKey,String... metricKey);

}
