<?php
//================================================================================
//------------------Required Libraries and Global Variables-----------------------
//================================================================================
require('bbb_api.php');
require('bbb_api_conf.php');


//================================================================================
//------------------------------------Main----------------------------------------
//================================================================================
echo '<?xml version="1.0"?>'."\r\n";
header('content-type: text/xml');

//Calls getMeetingXML and returns returns the result
echo BigBlueButton::getMeetingXML( $_GET['meetingID'], $url, $salt );
?>
