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

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Ticket;
import com.liferay.portal.model.User;
import com.liferay.portal.service.TicketLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

import javax.websocket.CloseReason;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author Miroslav Ligas
 */
@ServerEndpoint("/mapdata/{key}")
public class TwitterDataEndpoint {

	public static final int TICKET_TYPE_WEBSOCKET = 100333;

	private final static Log LOG = LogFactoryUtil.getLog(TwitterDataEndpoint.class);

	@OnOpen
	public void onOpen(Session session, @PathParam("key") String key)
		throws Exception {

		Ticket ticket = null;

		try {
			ticket = TicketLocalServiceUtil.getTicket(key);

			if (!validateTicket(ticket)) {
				session.close();
				return;
			}

			TwitterStatusListener.getInstance().registerSession(session);
		} catch (PortalException e) {
			session.close(new CloseReason(
				CloseReason.CloseCodes.VIOLATED_POLICY, "Unexpected Connection"));
		} finally {
			if (ticket != null) {
				TicketLocalServiceUtil.deleteTicket(ticket);
			}
		}
	}

	private static boolean validateTicket(Ticket ticket) {
		return
			ticket.getClassNameId() == PortalUtil.getClassNameId(User.class)
				&& ticket.getType() == TICKET_TYPE_WEBSOCKET
				&& !ticket.isExpired();
	}
}
