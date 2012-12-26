package ar.com.globallogic.sonarWebTool.services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.MetricQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import ar.com.globallogic.sonarWebTool.dtos.ProjectMetricDTO;
import ar.com.globallogic.sonarWebTool.exceptions.BusinessException;


public class SonarService implements IMetricService {

	@Value("${sonar.metric.default}")
	private String defaultMetric;
	
	@Value("${google.spreadSheet.dateFormat}")
	private String dateFormat;

	@Value("${sonar.defaultHost}")
	private String defaultUrl;
	
	@Value("${sonar.defaultuser}")
	private String defaultUser;
	
	@Value("${sonar.defaultPassword}")
	private String defaultPassword;
	
	private static Logger logger = Logger.getLogger(SonarService.class);
	
	@Override
	public List<ProjectMetricDTO> getMetricInfo(String url, String user, String password, String projectKey,String... metricKey) {
		
		logger.debug("Setting default values...");
		
		List<Resource> projectResources = new ArrayList<Resource>();
		
		try {
			String sonarServerUrl = chooseSonarServerUrl(url);
			String sonarUser = chooseSonarUser(user);
			String sonarPassword = chooseSonarPassword(password);
			String[] sonarMetrics = chooseMetricToExecute(metricKey);

			Sonar sonar = Sonar.create(sonarServerUrl, sonarUser, sonarPassword);

			validateMetricsKey(sonarMetrics, sonar);

			logger.info("Getting metrics from sonar server...");

			projectResources = buildResources(sonar, projectKey, sonarMetrics);

			logger.info("done!");

			logger.debug("Generating data transfer objects...");
		
		} catch (ConnectionException e) {
			logger.error("Sonar Connection refused");
			throw new BusinessException("Sonar Connection lost or refused",e);
		}
		
		List<ProjectMetricDTO> dtos = buildDtoList(projectResources);
		
		return dtos;
	}
	
	protected void validateMetricsKey(String[] sonarMetrics, Sonar sonar) {

		List<Metric> metrics = sonar.findAll(MetricQuery.all());
		
		Boolean found;
		
		for (int i = 0; i < sonarMetrics.length; i++) {
			found = false;
			String metricKey = sonarMetrics[i];

			Iterator<Metric> it = metrics.iterator();
			while (it.hasNext() && !found) {
				
				Metric metric = it.next();
				if (metric.getKey().equals(metricKey)) {
					found = true;
				}
			}
			
			if (!found) {
				logger.error("Invalid metrickey specified : " + metricKey);
				throw new BusinessException("Error : Invalid metrickey specified : " + metricKey);
			}
		}
	}

	protected List<ProjectMetricDTO> buildDtoList(List<Resource> projectResources) {
		
		List<ProjectMetricDTO> dtos = new ArrayList<ProjectMetricDTO>();
		
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		
		for (Resource resource : projectResources) {
			
			for (Measure measure : resource.getMeasures()) {
				ProjectMetricDTO projectMetricDTO = new ProjectMetricDTO(sdf.format(now),resource.getName(),measure.getMetricKey(),measure.getFormattedValue());
				dtos.add(projectMetricDTO);
			}
		}
		return dtos;
	}

	protected List<Resource> buildResources(Sonar sonar, String projectKey, String[] sonarMetrics){
		
		List<Resource> resources = new ArrayList<Resource>();

		if (projectKey != null) {

			Resource resource = sonar.find(ResourceQuery.createForMetrics(projectKey, sonarMetrics));

			if (resource == null) {
				logger.error("invalid project key specified : " + projectKey);
				throw new BusinessException("Error : invalid project key specified : " + projectKey);
			}

			resources.add(resource);
		} 
		else {
			resources.addAll(sonar.findAll(new ResourceQuery().setMetrics(sonarMetrics)));
		}

		return resources;
	}

	protected String[] chooseMetricToExecute(String... metricKey) {
		
		if (metricKey != null && metricKey.length != 0) {
			return metricKey;
		}
		
		logger.info("Metric parameter not received, the metric '" + defaultMetric + "' will be used");
		
		String[] metrics = new String[1];
		
		metrics[0] = defaultMetric;
		
		return metrics;
	}


	protected String chooseSonarPassword(String password) {
		
		if (StringUtils.hasText(password)) {
			return password;
		}
		return defaultPassword;
	}

	protected String chooseSonarUser(String user) {
		
		if (StringUtils.hasText(user)) {
			return user;
		} 
		
		logger.info("No user received,using default sonar user : "  + defaultUser);
		return defaultUser;
	}
	
	protected String chooseSonarServerUrl(String url) {

		if (StringUtils.hasText(url)){
			return url;
		}
		
		logger.info("No sonar server host received,using the default one  : "  + defaultUrl);
		return defaultUrl;
	}
}
