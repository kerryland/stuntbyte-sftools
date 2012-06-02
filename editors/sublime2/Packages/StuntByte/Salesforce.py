import sublime, sublime_plugin
import subprocess
import os
execcmd = __import__("exec")



# Command for build process to run
class SfExecCommand(execcmd.ExecCommand):
	def run(self, *args, **kwargs):
		cmd = salesforceCommand(self);
		execParams = cmd.buildCommand(self.window.active_view(), "-compile");
		args = execParams

		super(SfExecCommand, self).run(args, 
			'^(...*?)>([0-9]*):?([0-9]*)(.*)',
			 kwargs)
		sublime.status_message("Done!")



# Generic "run an ide command" method

class salesforceCommand(sublime_plugin.TextCommand):

	def run(self, edit, cmd, file):
		self.runSimple(self.view, cmd);


	def runSimple(self, view, cmd):	
		execCommand = self.buildCommand(view, cmd);
		#sublime.status_message(execCommand.size());
		cmd = execcmd.ExecCommand(view.window());
		cmd.run(execCommand, []);
		sublime.status_message("Done!")


	def buildCommand(self, view, cmd):

		projProps = view.settings().get("ide.properties");
		if (projProps == None) :
			sublime.error_message("ide.properties not defined in project");

		#projProps = os.path.dirname(view.file_name());
		#projProps = os.path.join(projProps, os.path.pardir);
		#projProps = os.path.join(projProps, os.path.pardir);
		#projProps = os.path.join(projProps, "ide.properties");
		#projProps = os.path.realpath(projProps);
		if os.path.exists(projProps) == False :
			sublime.error_message(projProps + " does not exist");

		result = [];
		result.append("java")
		result.append("-cp")
		result.append(view.settings().get("stuntbyte_jar"))
		result.append("com.stuntbyte.salesforce.ide.SalesfarceIDE")		
		result.append(projProps)
		result.append(os.path.dirname(projProps)  + "/.tags")
		
		bufferName = str(view.file_name())
		
		sublime.status_message("Running" + cmd+ "...")

		result.append(cmd)
		if(view.file_name() != None):
			result.append(bufferName)
		    
		return result
