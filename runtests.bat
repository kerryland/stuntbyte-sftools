call ant compile
REM java -cp lib/ant-salesforce.jar;. com.stuntbyte.salesforce.ide.SalesfarceIDE force\classes\AccountHierarchyTestData.cls  --download
rem java -cp lib/ant-salesforce.jar;classes com.stuntbyte.salesforce.ide.SalesfarceIDE force\classes\AccountHierarchyTestData.cls %*
REM java -cp lib/ant-salesforce.jar;classes com.stuntbyte.salesforce.ide.SalesfarceIDE force\classes\AccountHierarchyTestData.cls %*
REM java -cp lib/ant-salesforce.jar;classes com.stuntbyte.salesforce.ide.SalesfarceIDE force\triggers\CaseAfterInsUpdTrigger.trigger %*


java -cp  c:/apps/sql-workbench/drivers/sfdc-kjs-driver-2.jar;classes com.stuntbyte.salesforce.ide.SalesfarceIDE -runtests
