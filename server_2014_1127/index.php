<?php


require_once("mysql.class.php");
$dbHost = "localhost";
$dbUsername = "x003540_db";
$dbPassword = "FUlnFXCF5H";
$dbName = "x003540_db";


$db = new MySQL($dbHost,$dbUsername,$dbPassword,$dbName);

define("FAILED", 0);
define("FAILED2",33);
define("SUCCESSFUL", 1);
define("SIGN_UP_USERNAME_CRASHED", 2);  
define("ADD_NEW_USERNAME_NOT_FOUND", 2);

define("TIME_INTERVAL_FOR_USER_STATUS", 60);

define("USER_APPROVED", 1);
define("USER_UNAPPROVED", 0);


$username = (isset($_REQUEST['username']) && count($_REQUEST['username']) > 0) 
							? $_REQUEST['username'] 
							: NULL;
$android_id = isset($_REQUEST['android_id']) ? $_REQUEST['android_id'] : NULL;
$password = isset($_REQUEST['password']) ? md5($_REQUEST['password']) : NULL;
$port = isset($_REQUEST['port']) ? $_REQUEST['port'] : NULL;
$loc_lat = isset($_REQUEST['loc_lat']) ? $_REQUEST['loc_lat'] : NULL;
$loc_long = isset($_REQUEST['loc_long']) ? $_REQUEST['loc_long'] : NULL;

$action = isset($_REQUEST['action']) ? $_REQUEST['action'] : NULL;
if ($action == "testWebAPI")
{
	if ($db->testconnection()){
	echo SUCCESSFUL;
	exit;
	}else{
	echo FAILED;
	exit;
	}
}


$out = NULL;

error_log($action."\r\n", 3, "error.log");
switch($action) 
{
	
	case "authenticateUser":
		if ($userId = authenticateUser($db, $android_id)) 
		{					
			
			$sqlmessage = "SELECT m.id, m.sentdt, m.messagetext, m.android_id from messages m \n";
					$out .= "<data>"; 
						if ($resultmessage = $db->query($sqlmessage))			
							{
							while ($rowmessage = $db->fetchObject($resultmessage))
								{
								$out .= "<message  from='".$rowmessage->android_id."'  sendt='".$rowmessage->sentdt."' text='".$rowmessage->messagetext."' />";
								}
							}
					$out .= "</data>";
		}
		else
		{
				$android_id = $_REQUEST['android_id'];		
			 	
			 $sql = "select Id from  users 
			 				where android_id = '".$android_id."' limit 1";
			 if ($result = $db->query($sql))
			 {
			 		if ($db->numRows($result) == 0) 
			 		{
			 				$sql = "insert into users (android_id) values ('".$android_id."') ";		 					
						 					
			 					error_log("$sql", 3 , "error_log");
							if ($db->query($sql))	
							{
							 		$out = SUCCESSFUL;
							}				
							else {
									$out = $sql;
							}				 			
			 		}
			 		else
			 		{
			 			$out = SIGN_UP_USERNAME_CRASHED;
			 		}
			 }	
			 
			 
			 
			 
			 
			 
				$sqlmessage = "SELECT m.id, m.sentdt, m.messagetext, m.android_id from messages m \n";
			$out .= "<data>"; 
				if ($resultmessage = $db->query($sqlmessage))			
					{
					while ($rowmessage = $db->fetchObject($resultmessage))
						{
						$out .= "<message  from='".$rowmessage->android_id."'  sendt='".$rowmessage->sentdt."' text='".$rowmessage->messagetext."' />";
						}
					}
			$out .= "</data>";
		}
		
	
	
	break;
	case "signUpUser":
		if (isset($_REQUEST['android_id']))
		{
			 $android_id = $_REQUEST['android_id'];		
			 	
			 $sql = "select Id from  users 
			 				where android_id = '".$android_id."' limit 1";
			 if ($result = $db->query($sql))
			 {
			 		if ($db->numRows($result) == 0) 
			 		{
			 				$sql = "insert into users (android_id) values ('".$android_id."') ";		 					
						 					
			 					error_log("$sql", 3 , "error_log");
							if ($db->query($sql))	
							{
							 		$out = SUCCESSFUL;
							}				
							else {
									$out = $sql;
							}				 			
			 		}
			 		else
			 		{
			 			$out = SIGN_UP_USERNAME_CRASHED;
			 		}
			 }				 	 	
		}
		else
		{
			$out = FAILED;
		}	
	break;
	case "sendMessage":
			 $message = $_REQUEST['message'];	
			 $loc_lat = $_REQUEST['loc_lat'];
			$loc_long = $_REQUEST['loc_long'];
				
					$sql22 = "INSERT INTO messages (android_id, sentdt, messagetext, loc_lat,loc_long) 
					VALUES ('".$android_id."','".DATE("Y-m-d H:i")."', '".$message."', '".$loc_lat."', '".$loc_long."');";						
						 					
			 		error_log("$sql22", 3 , "error_log");
					if ($db->query($sql22))	
					{
						$out = SUCCESSFUL;
					}				
					else 
					{
						$out = FAILED;
					}				 		
			$sqlto = NULL;
	break;
	case "updateUsername":
			$username = $_REQUEST['username'];
			$android_id = $_REQUEST['android_id'];
			$sql22= "select * from users where android_id = '".$android_id."' limit 1";
			$out = NULL;
			if ($result22 = $db->query($sql22))
			{
				if ($row22 = $db->fetchObject($result22))
				{
					$out = $row22->Id;
					$sql22 = "update users set authenticationTime = NOW(),
						IP = '".$_SERVER["REMOTE_ADDR"]."',
						port = 15145,
						username = '".$username."'
						where Id= ".$row22->Id."
						limit 1";
					if ($db->query($sql22))	
					{
						$out = SUCCESSFUL;
					}				
					else 
					{
						$out = FAILED2;
					}	
				}	
			}
	break;
	
	case "locationUpdate":
			$android_id = $_REQUEST['android_id'];
			$loc_lat = $_REQUEST['loc_lat'];
			$loc_long = $_REQUEST['loc_long'];
			$sql22= "select * from users where android_id = '".$android_id."' limit 1";
			$out = NULL;
			if ($result22 = $db->query($sql22))
			{
				if ($row22 = $db->fetchObject($result22))
				{
					$out = $row22->Id;
					$sql22 = "update users set authenticationTime = NOW(),
						IP = '".$_SERVER["REMOTE_ADDR"]."',
						port = 15145,
						loc_lat = '".$loc_lat."',
						loc_long = '".$loc_long."'
						where Id= ".$row22->Id."
						limit 1";
					if ($db->query($sql22))	
					{
						$out = SUCCESSFUL;
					}				
					else 
					{
						$out = FAILED2;
					}	
				}	
			}
	
	break;
	
	default:
		$out = "altalanos hiba!";		
		break;	
}

echo $out;



//auth for android_id
function authenticateUser($db, $android_id)
{

	$sql22= "select * from users where android_id = '".$android_id."' limit 1";
	$out = NULL;
	if ($result22 = $db->query($sql22))
	{
		if ($row22 = $db->fetchObject($result22))
		{
			$out = $row22->Id;
			$sql22 = "update users set authenticationTime = NOW(),
				IP = '".$_SERVER["REMOTE_ADDR"]."',
				port = 15145
				where Id= ".$row22->Id."
				limit 1";
			$db->query($sql22);
		}	
	}
	return $out;
}
?>