Alter Table dataTable
add visualizationEnabled boolean;

update dataTable set  visualizationEnabled = false;


INSERT INTO pagedef ( name, path, role_id, networkrole_id ) VALUES ( 'EditStudyFilesPage','/study/EditStudyFilesPage.xhtml',1,null );
