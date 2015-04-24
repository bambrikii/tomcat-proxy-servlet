package ru.sovzond.mgis.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/**
 * Servlet implementation class ProxyServlet
 */
@WebServlet(urlPatterns = "/*", initParams = { @WebInitParam(name = "targetHost", value = "localhost"),
		@WebInitParam(name = "targetPort", value = "8082"), @WebInitParam(name = "targetPath", value = "") })
public class ProxyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private String targetHost = "localhost";
	private int targetPort = 8082;

	@Override
	public void init(ServletConfig config) throws ServletException {
		targetHost = config.getInitParameter("targetHost");
		targetPort = Integer.parseInt(config.getInitParameter("targetPort"));
		super.init(config);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp);
	}

	private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException, ClientProtocolException {
		CloseableHttpClient httpClient = HttpClients.createMinimal();

		String scheme = req.getScheme();
		String queryString = req.getQueryString();
		String path = req.getRequestURI();// .substring(req.getContextPath().length());

		StringBuilder sb = new StringBuilder();
		sb.append(scheme).append("://").append(targetHost).append(":").append(targetPort).append(path);
		if (queryString != null) {
			sb.append("?").append(queryString);
		}
		String newUrl = sb.toString();
		System.out.println("newUrl: " + newUrl);

		HttpUriRequest request = new HttpGet(newUrl);
		HttpParams params = new BasicHttpParams();
		params.setParameter(ClientPNames.HANDLE_REDIRECTS, true);
		request.setParams(params);
		Enumeration<String> headerNames = req.getHeaderNames();
		// request.setHeader("host", "localhost:8082");
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			if (!headerName.equals("host") && !headerName.equals("referer")) {
				request.setHeader(headerName, req.getHeader(headerName));
			}
		}
		CloseableHttpResponse response = httpClient.execute(request);
		Header[] headers = response.getAllHeaders();
		for (Header header : headers) {
			resp.setHeader(header.getName(), header.getValue());
		}
		int statusCode = response.getStatusLine().getStatusCode();
		System.out.println("statusCode: " + statusCode);
		resp.setStatus(statusCode);
		InputStream is = response.getEntity().getContent();
		OutputStream os = resp.getOutputStream();
		resp.setContentLength(IOUtils.copy(is, os));
	}

}
