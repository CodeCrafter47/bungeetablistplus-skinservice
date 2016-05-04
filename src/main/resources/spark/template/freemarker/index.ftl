<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <meta name="description" content="">
    <meta name="author" content="">
    <link rel="icon" href="favicon.png">

    <title>BungeeTabListPlus SkinService</title>

    <!-- Bootstrap core CSS -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap theme -->
    <link href="css/bootstrap-theme.min.css" rel="stylesheet">

    <!-- Custom styles for this template -->
    <link href="theme.css" rel="stylesheet">

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
</head>

<body role="document">

<!-- Fixed navbar -->
<nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed"
                    data-toggle="collapse" data-target="#navbar"
                    aria-expanded="false" aria-controls="navbar">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">BungeeTabListPlus SkinService</a>
        </div>
        <div id="navbar" class="navbar-collapse collapse">
            <ul class="nav navbar-nav">
                <li class="active"><a href="#">Home</a></li>
                <li><a href="#about">About</a></li>
                <li><a href="#status">Stats</a></li>
                <li>
                    <a href="https://github.com/CodeCrafter47/bungeetablistplus-skinservice">Github</a>
                </li>
            </ul>
        </div><!--/.nav-collapse -->
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="alert alert-success" role="alert">
        This service allows user of the BungeeTabListPlus plugin to use any
        image for the heads in the tab list.
    </div>

    <div class="page-header">
        <h1><a name="about"></a>About</h1>
    </div>
    <p>This is an api for registering minecraft skins with a specified image on
        their head on mojang servers. This is used by the <a
                href="https://www.spigotmc.org/resources/bungeetablistplus.313/">BungeeTabListPlus</a>
        plugin to allow the users to use custom images for the heads in the tab
        list.</p>

    <div class="page-header">
        <h1><a name="status"></a>Stats</h1>
    </div>
    <!-- todo -->
    <div class="row">
        <div class="col-md-12">
            <table class="table">
                <thead>
                <tr>
                    <th>Type</th>
                    <th>7 days</th>
                    <th>24 hours</th>
                    <th>1 hour</th>
                    <th>5 minutes</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>Avrg. queue size</td>
                    <td>${avrgQueueSize10080}</td>
                    <td>${avrgQueueSize1440}</td>
                    <td>${avrgQueueSize60}</td>
                    <td>${avrgQueueSize5}</td>
                </tr>
                <tr>
                    <td>Max. queue size</td>
                    <td>${maxQueueSize10080}</td>
                    <td>${maxQueueSize1440}</td>
                    <td>${maxQueueSize60}</td>
                    <td>${maxQueueSize5}</td>
                </tr>
                <tr>
                    <td>API requests</td>
                    <td>${requests10080}</td>
                    <td>${requests1440}</td>
                    <td>${requests60}</td>
                    <td>${requests5}</td>
                </tr>
                <tr>
                    <td>API requests answered from cache</td>
                    <td>${cachedRequests10080}</td>
                    <td>${cachedRequests1440}</td>
                    <td>${cachedRequests60}</td>
                    <td>${cachedRequests5}</td>
                </tr>
                <tr>
                    <td>Skins registered to Mojang</td>
                    <td>${mojangRequests10080}</td>
                    <td>${mojangRequests1440}</td>
                    <td>${mojangRequests60}</td>
                    <td>${mojangRequests5}</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</div> <!-- /container -->


<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script src="js/bootstrap.min.js"></script>
</body>
</html>
