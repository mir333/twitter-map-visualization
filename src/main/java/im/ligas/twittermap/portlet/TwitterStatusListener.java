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

import com.liferay.portal.kernel.concurrent.ConcurrentHashSet;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import twitter4j.GeoLocation;
import twitter4j.Place;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * @author Miroslav Ligas
 */
public class TwitterStatusListener implements StatusListener {

	private final static Log LOG = LogFactoryUtil.getLog(TwitterStatusListener.class);

	private final static TwitterStatusListener INSTANCE = new TwitterStatusListener();

	private Set<Session> sessions;

	private TwitterStatusListener() {
		sessions = new ConcurrentHashSet<Session>();
	}

	@Override
	public void onStatus(Status status) {
		Place place = status.getPlace();
		if (place != null) {
			String iso3Country = new Locale(status.getLang(), status.getPlace().getCountryCode()).getISO3Country();
			JSONObject json = JSONFactoryUtil.createJSONObject();
			json.put("country", iso3Country);
			GeoLocation[][] coordinates = place.getBoundingBoxCoordinates();
			GeoLocation location = coordinates[0][0];
			json.put("lat", location.getLatitude());
			json.put("lng", location.getLongitude());
			LOG.debug(json);
			broadcast(json.toString());
		}
	}

	@Override
	public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
	}

	@Override
	public void onTrackLimitationNotice(int limit) {
	}

	@Override
	public void onScrubGeo(long user, long upToStatus) {
	}

	@Override
	public void onStallWarning(StallWarning warning) {
	}

	@Override
	public void onException(Exception e) {
	}


	public void registerSession(Session session) {
		sessions.add(session);
	}

	public void unregisterSession(String id) {
		sessions.remove(id);
	}

	public static TwitterStatusListener getInstance() {
		return INSTANCE;
	}


	private void broadcast(String message) {
		Iterator<Session> iterator = sessions.iterator();
		while (iterator.hasNext()) {
			Session session = iterator.next();
			try {
				if (session.isOpen()) {
					session.getBasicRemote().sendText(message);
				} else {
					iterator.remove();
				}
			} catch (IOException e) {
				LOG.error("Could not sent message", e);
			}
		}
	}
}
