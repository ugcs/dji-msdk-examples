package com.example.ugcssample.drone.domain

import dji.common.mission.waypoint.WaypointMission


data class RouteModel(
    val actions: List<Any>?,
    val autopilotModel: AutopilotModel?,
    val mission: WaypointMission?)
