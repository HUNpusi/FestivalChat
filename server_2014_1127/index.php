<?php
/****************************************
*		Server of Android IM Application
*
*		Author: ahmet oguz mermerkaya
* 		Email: ahmetmermerkaya@hotmail.com
*		Editor: Dominik Pirngruber
*		Email: d.pirngruber@gmail.com
* 		Date: Jun, 25, 2013   	
* 	
*		Supported actions: 
*			1.  authenticateUser
*			    if user is authentiated return friend list
* 		    
*			2.  signUpUser
* 		
*			3.  addNewFriend
* 		
* 			4.  responseOfFriendReqs
*
*			5.  testWebAPI
*************************************/


//TODO:  show error off

require_once("mysql.class.php");
$dbHost = "localhost";
$dbUsername = "x003540_db";
$dbPassword = "FUlnFXCF5H";
$dbName = "x003540_db";


$db = new MySQL($dbHost,$dbUsername,$dbPassword,$dbName);

// if operation is failed by unknown reason
define("FAILED", 0);
define("FAILED2",33);

define("SUCCESSFUL", 1);
// when  signing up, if username is already taken, return this error
define("SIGN_UP_USERNAME_CRASHED", 2);  
// when add new friend request, if friend is not found, return this error 
define("ADD_NEW_USERNAME_NOT_FOUND", 2);

// TIME_INTERVAL_FOR_USER_STATUS: if last authentication time of user is older 
// than NOW - TIME_INTERVAL_FOR_USER_STATUS, then user is considered offline
define("TIME_INTERVAL_FOR_USER_STATUS", 60);

define("USER_APPROVED", 1);
define("USER_UNAPPROVED", 0);


$username = (isset($_REQUEST['username']) && count($_REQUEST['username']) > 0) 
							? $_REQUEST['username'] 
							: NULL;
$android_id = isset($_REQUEST['android_id']) ? $_REQUEST['android_id'] : NULL;
$password = isset($_REQUEST['password']) ? md5($_REQUEST['password']) : NULL;
$port = isset($_REQUEST['port']) ? $_REQUEST['port'] : NULL;

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

//if ($username == NULL || $password == NULL)	 
//{
//	echo FAILED;
//	exit;
//}

$out = NULL;

error_log($action."\r\n", 3, "error.log");
switch($action) 
{
	
	case "authenticateUser":
		if ($userId = authenticateUser($db, $android_id)) 
		{					
			
			// providerId and requestId is Id of  a friend pair,
			// providerId is the Id of making first friend request
			// requestId is the Id of the friend approved the friend request made by providerId
			
			// fetching friends, 
			// left join expression is a bit different, 
			//		it is required to fetch the friend, not the users itself
			
			//$sql = "select u.Id, u.android_id, (NOW()-u.authenticationTime) as //authenticateTimeDifference, u.IP, u.port 
			//				from users u";
										 
			//$sqlmessage = "SELECT * FROM `messages` WHERE `touid` = ".$userId." AND `read` = 0 LIMIT 0, 30 ";
			
			$sqlmessage = "SELECT m.id, m.sentdt, m.messagetext, m.android_id from messages m \n";
   // . "left join users u on u.Id = m.fromuid WHERE `touid` = ".$userId." AND `read` = 0 LIMIT 0, 30 ";
			
	
			//if ($result = $db->query($sqlmessage))			
			//{
			
					$out .= "<data>"; 
					//$out .= "<user userKey='".$userId."' />";
					//while ($row = $db->fetchObject($result))
					//{
						//$status = "offline";
						//if (((int)$row->status) == USER_UNAPPROVED)
						//{
						//	$status = "unApproved";
						//}
						//else
						//ez gyó lehet, de elsõnek menjen a sima szoba
						//if (((int)$row->authenticateTimeDifference) < TIME_INTERVAL_FOR_USER_STATUS)
						//{
						//	$status = "online";
						//}
						//$out .= "<friend  username = '".$row->username."'  status='".$status."' //IP='".$row->IP."' userKey = '".$row->Id."'  port='".$row->port."'/>";
												
												// to increase security, we need to change userKey periodically and pay more attention
												// receiving message and sending message 
						
					//}
					
						if ($resultmessage = $db->query($sqlmessage))			
							{
							while ($rowmessage = $db->fetchObject($resultmessage))
								{
								$out .= "<message  from='".$rowmessage->android_id."'  sendt='".$rowmessage->sentdt."' text='".$rowmessage->messagetext."' />";
								//$sqlendmsg = "UPDATE `messages` SET `read` = 1, `readdt` = '".DATE("Y-m-d H:i")."' WHERE `messages`.`id` = ".$rowmessage->id.";";
								//$db->query($sqlendmsg);
								
								}
							}
					$out .= "</data>";
					
			//}
			//else
			//{
			//	$out = FAILED2;
			//}			
		}
		else
		{
				// exit application if not authenticated user
				$out = FAILED;
		}
		
	
	
	break;
	//save new client's android_id
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
	//a "messages" táblába belevágni az üzit, hogy kitõl jött, mikor, mi.
	case "sendMessage":
	//if ($userId = authenticateUser($db, $android_id)) 
		//{	
			 $message = $_REQUEST['message'];	
				
					$sql22 = "INSERT INTO messages (android_id, sentdt, messagetext) 
					VALUES ('".$android_id."','".DATE("Y-m-d H:i")."', '".$message."');";						
						 					
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
		//}
		//else
		//{
		//	$out = FAILED;
		//}	
	break;
	
	
	default:
		$out = FAILED;		
		break;	
}

echo $out;



///////////////////////////////////////////////////////////////
/*function authenticateUser($db, $username, $password)
{
	
	$sql22 = "select * from users 
					where username = '".$username."' and password = '".$password."' 
					limit 1";
	
	$out = NULL;
	if ($result22 = $db->query($sql22))
	{
		if ($row22 = $db->fetchObject($result22))
		{
				$out = $row22->Id;
				
				$sql22 = "update users set authenticationTime = NOW(), 
																 IP = '".$_SERVER["REMOTE_ADDR"]."' ,
																 port = 15145 
								where Id = ".$row22->Id."
								limit 1";
				
				$db->query($sql22);				
								
								
		}		
	}
	
	return $out;
}


//Changed auth for android_id
*/function authenticateUser($db, $android_id)
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