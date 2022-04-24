package com.example.ugcssample.utils

import android.content.Context
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class ExceptionWriter(val exception : Throwable?)
{
	fun saveStackTraceToSd(context : Context)
	{
		try
		{
			val out = PrintStream(getExceptionFileStream(context), true, StandardCharsets.UTF_8.toString())
			exception!!.printStackTrace(out)
			out.close()
		}
		catch (excep : Exception)
		{
			excep.printStackTrace()
		}
	}
	private fun getPath(context : Context) : String
	{
		val dataDir = context.getExternalFilesDir(null)
		return "${dataDir?.absolutePath}${File.separator}"
	}
	@Throws(FileNotFoundException::class)
	fun getExceptionFileStream(context : Context) : FileOutputStream
	{
		val myDir = File(getPath(context))
		myDir.mkdirs()
		val file = File(myDir, getTimeStamp() + ".txt")
		if (file.exists()) file.delete()
		return FileOutputStream(file)
	}
	fun getTimeStamp() : String?
	{
		val sdf = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.US)
		return sdf.format(Date())
	}
	
	companion object {
		@JvmStatic
		fun setupCrashHandler (context : Context) {
			val exceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
			
			val dpExceptionHandler = Thread.UncaughtExceptionHandler { thread, ex ->
				ExceptionWriter(ex).saveStackTraceToSd(context)
				exceptionHandler?.uncaughtException(thread, ex)
			}
			
			Thread.setDefaultUncaughtExceptionHandler(dpExceptionHandler)
			
		}
	}
}