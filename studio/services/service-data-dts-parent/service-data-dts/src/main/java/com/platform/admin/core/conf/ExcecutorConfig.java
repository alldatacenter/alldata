package com.platform.admin.core.conf;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class ExcecutorConfig implements InitializingBean, DisposableBean {

	private static ExcecutorConfig excecutorConfig = null;

	public static ExcecutorConfig getExcecutorConfig() {
		return excecutorConfig;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		excecutorConfig = this;
	}

	@Override
	public void destroy() throws Exception {
	}

	@Value("${dts.executor.chunjunHome}")
	private  String flinkxHome;



	@Value("${dts.executor.chunjunjsonPath}")
	private  String flinkxjsonPath;

	@Value("${dts.executor.chunjunlogHome}")
	private  String flinkxlogHome;

	@Value("${dts.executor.dataxHome}")
	private String dataxHome;

	@Value("${dts.executor.dataxjsonPath}")
	private String dataxjsonPath;

	@Value("${dts.executor.dataxlogHome}")
	private String dataxlogHome;

	@Value("${common.mysql.dts.url}")
	private String url;

	@Value("${common.mysql.dts.driver-class-name}")
	private String driverClassname;

	@Value("${common.mysql.dts.username}")
	private String username;

	@Value("${common.mysql.dts.password}")
	private String password;


	public static void setExcecutorConfig(ExcecutorConfig excecutorConfig) {
		ExcecutorConfig.excecutorConfig = excecutorConfig;
	}

	public String getFlinkxHome() {
		return flinkxHome;
	}

	public void setFlinkxHome(String flinkxHome) {
		this.flinkxHome = flinkxHome;
	}

	public String getFlinkxjsonPath() {
		return flinkxjsonPath;
	}

	public void setFlinkxjsonPath(String flinkxjsonPath) {
		this.flinkxjsonPath = flinkxjsonPath;
	}

	public String getFlinkxlogHome() {
		return flinkxlogHome;
	}

	public void setFlinkxlogHome(String flinkxlogHome) {
		this.flinkxlogHome = flinkxlogHome;
	}

	public String getDataxHome() {
		return dataxHome;
	}

	public void setDataxHome(String dataxHome) {
		this.dataxHome = dataxHome;
	}

	public String getDataxjsonPath() {
		return dataxjsonPath;
	}

	public void setDataxjsonPath(String dataxjsonPath) {
		this.dataxjsonPath = dataxjsonPath;
	}

	public String getDataxlogHome() {
		return dataxlogHome;
	}

	public void setDataxlogHome(String dataxlogHome) {
		this.dataxlogHome = dataxlogHome;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDriverClassname() {
		return driverClassname;
	}

	public void setDriverClassname(String driverClassname) {
		this.driverClassname = driverClassname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
