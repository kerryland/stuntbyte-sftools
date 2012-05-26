import sublime, sublime_plugin
import subprocess
import os
execcmd = __import__("exec")



# Command for build process to run

#class SfExecCommand(sublime_plugin.WindowCommand):
class SfExecCommand(execcmd.ExecCommand):
	def run(self, *args, **kwargs):
		print args
		print kwargs
		print kwargs
		print kwargs
		print kwargs
		print kwargs

		cmd = salesforceCommand(self);
		execParams = cmd.buildCommand(self.window.active_view(), "-compile");

		print execParams

		args = execParams

		super(SfExecCommand, self).run(args, 
			'^(...*?)>([0-9]*):?([0-9]*)(.*)',
			
			 kwargs)
		super(SfExecCommand, self).run(args, kwargs)



# Generic "run an ide command" method

class salesforceCommand(sublime_plugin.TextCommand):

	def run(self, edit, cmd, file):
		self.runSimple(self.view, cmd);


	def runSimple(self, view, cmd):	
		execCommand = self.buildCommand(view, cmd);

		cmd = execcmd.ExecCommand(view.window());
		cmd.run(execCommand, []);

		# Results = doSystemCommand(execCommand)

		# displayResults(Results, "MessageBox", view)
		sublime.status_message("Done!")



	def buildCommand(self, view, cmd):	
		projProps=os.environ["STUNTBYTE_PROJ"]
		
		result = [];
		result.append("java")
		result.append("-cp")
		result.append(view.settings().get("stuntbyte_jar"))
		result.append("com.stuntbyte.salesforce.ide.SalesfarceIDE")
		result.append(projProps)
		result.append(os.path.dirname(projProps)  + "/tags")


		farceIde="java -cp " + view.settings().get("stuntbyte_jar") + " com.stuntbyte.salesforce.ide.SalesfarceIDE " +  projProps + " " + os.path.dirname(projProps) + "/tags"		
		#sublime.error_message(farceIde)

		#farceIde="gvim "


		# don't operate unless the buffer is saved
		if(view.file_name() == None):
			return

		# Put the file name in quotes to allow spaces in the name			
		# bufferName = "\"" + str(view.file_name()) + "\""
		bufferName = str(view.file_name())
		
		sublime.status_message("Running" + cmd+ "...")

		# return farceIde + ' ' + cmd + ' ' + bufferName
		result.append(cmd)
		result.append(bufferName)
		return result


# Runs a system command from the command line
# Captures and returns both stdout and stderr as an array, in that respective order
def doSystemCommand(commandText):
	p = subprocess.Popen(commandText, shell=True, bufsize=1024, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	p.wait()
	stdout = p.stdout
	stderr = p.stderr
	return [stdout.read(),stderr.read()]


# Displays given stderr if its not blank, otherwise displays stdout
# Method of display is configured using the Mode argument
#
# Results is of the form
# Results[0] is stdout to display
# Results[1] is stderr which, if its not None, will be displayed instead of stdout
#
# Modes:
# 	Window - Opens a new buffer with output
#	MessageBox - Creates a messageBox with output
#
# view is the view that will be used to create new buffers
def displayResults(Results, Mode, view):
	if(Results[1] != None and Results[1] != ""):
		print(str(Results[1]))
		sublime.error_message(str(Results[1]))  
	if(Results[0] != None and Results[0] != ""):
		print(str(Results[0]))
		#sublime.error_message(str(Results[0]))