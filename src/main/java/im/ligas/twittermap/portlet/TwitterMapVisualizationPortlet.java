/**
 * Copyright (c) 2015 Miroslav Ligas All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package im.ligas.twittermap.portlet;

import com.google.common.collect.Lists;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Ticket;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.TicketLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.util.portlet.PortletProps;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import com.twitter.hbc.twitter4j.Twitter4jStatusClient;
import im.ligas.util.bridges.mvc.MVCPortlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Miroslav Ligas
 */
public class TwitterMapVisualizationPortlet extends MVCPortlet {
	private final static Log LOG = LogFactoryUtil.getLog(TwitterDataEndpoint.class);
	Twitter4jStatusClient client;

	@Override
	public void setModel(Map<String, Object> model, RenderRequest request) {
		ThemeDisplay td = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);

		long mls = (new Date()).getTime() + 20000;

		Ticket ticket = null;
		try {
			ticket = TicketLocalServiceUtil.addTicket(
					td.getCompanyId(), User.class.getName(),
					td.getUserId(),
					TwitterDataEndpoint.TICKET_TYPE_WEBSOCKET, null,
					new Date(mls),
					new ServiceContext());

			String wsUrl = String.format("ws://%s:%s%s/mapdata/%s",
					td.getServerName(), td.getServerPort(),
					request.getContextPath(), ticket.getKey());

			model.put("websocketURL", wsUrl);

		} catch (SystemException e) {
			LOG.error("unable to generate token.");
		}

	}


	public void startStreaming(ResourceRequest request, ResourceResponse response) {
		start();
	}

	public void stopStreaming(ResourceRequest request, ResourceResponse response) {
		client.stop();
	}


	private Twitter4jStatusClient getTwitterConnection() {
		String consumerKey = PortletProps.get("consumer.key");
		String consumerSecret = PortletProps.get("consumer.secret");
		String token = PortletProps.get("token");
		String secret = PortletProps.get("secret");

		BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);

		StatusesSampleEndpoint endpoint = new StatusesSampleEndpoint();

		Authentication auth = new OAuth1(consumerKey, consumerSecret, token, secret);

		BasicClient client = new ClientBuilder()
			.hosts(Constants.STREAM_HOST)
			.endpoint(endpoint)
			.authentication(auth)
			.processor(new StringDelimitedProcessor(queue))
			.build();

		ExecutorService service = Executors.newSingleThreadExecutor();

		Twitter4jStatusClient t4jClient = new Twitter4jStatusClient(
			client, queue, Lists.newArrayList(TwitterStatusListener.getInstance()), service);
		return t4jClient;
	}

	private void start() {
		try {
			client = getTwitterConnection();
			client.connect();
			client.process();
		} catch (Exception e) {
			LOG.error(e);
		}
	}


}
