package com.example.ugcssample

import dji.common.error.DJIError
import java.lang.Exception

class DJIErrorException : Exception {
    val errorCode: Int
    val description : String

    constructor(djiError: DJIError) : super(djiError.description) {
        errorCode = djiError.errorCode;
        description = djiError.description
    }
    
}