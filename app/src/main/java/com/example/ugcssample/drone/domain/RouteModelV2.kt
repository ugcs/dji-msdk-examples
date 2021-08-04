package com.example.ugcssample.drone.domain

import dji.common.mission.waypointv2.Action.WaypointV2Action
import dji.common.mission.waypointv2.WaypointV2Mission

data class RouteModelV2(
        val actions: List<WaypointV2Action>?,
        val autopilotModel: AutopilotModel?,
        val mission: WaypointV2Mission?
                     )