<%--
/**
 * Copyright (c) 2015 Miroslav Ligas All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<portlet:defineObjects/>
<c:set var="ns"><portlet:namespace/></c:set>
<portlet:resourceURL id="startStreaming" var="startUrl"/>

<div class="control-buttons">
	<a class="stop btn" href="<portlet:resourceURL id="stopStreaming"/>">Stop</a>
</div>
<div class="control-log">
	<span class="text"></span>
</div>
<div id="${ns}map_canvas" class="google-map">

</div>

<script type="text/javascript" src="http://maps.google.com/maps/api/js?libraries=visualization"></script>
<script type="text/javascript">

	AUI().ready('node', 'io', function (A) {

		var liferay_ws;
		var pointArray;

		function init(){
			A.io('${startUrl}', {data: new Date()});
			_attacheWS();

			A.one(".control-buttons a.stop").on('click', function (event) {
				event.preventDefault();
				var url = this.attr('href');
				A.io(url, {data: new Date()});
				liferay_ws.close();
			});

		}
		function initMap() {
			pointArray = new google.maps.MVCArray([]);

			var map = new google.maps.Map(
					document.getElementById("${ns}map_canvas"),
					{
						zoom: 2,
						mapTypeId: google.maps.MapTypeId.ROADMAP,
						center: new google.maps.LatLng(51.507335, -0.127683),
						scrollwheel: false,
						navigationControl: false,
						mapTypeControl: false,
						scaleControl: false,
						draggable: false,
					});

			var heatmap = new google.maps.visualization.HeatmapLayer({
				data: pointArray,
				radius: 5,
				maxIntensity: 10,
				dissipating: false,
			});
			heatmap.setMap(map);
		}


		function _attacheWS() {
			liferay_ws = new WebSocket('${websocketURL}');

			console.log(liferay_ws);

			liferay_ws.onmessage = function (evt) {
				var data = JSON.parse(evt.data);
				A.one(".control-log .text").html(data.country);
				pointArray.push(new google.maps.LatLng(data.lat, data.lng));
			};
		}

		init();
		initMap();
	});

</script>

