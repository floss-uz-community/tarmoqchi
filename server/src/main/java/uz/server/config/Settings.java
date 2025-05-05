package uz.server.config;

public class Settings {
    public static final String HOST = "tarmoqchi.uz";

    public static String NOT_RUNNING_APP_OF_CLIENT_HTML = """
            <!DOCTYPE html>
                   <html lang="en">
                   <head>
                     <meta charset="UTF-8">
                     <meta name="viewport" content="width=device-width, initial-scale=1.0">
                     <title>Application Not Running</title>
                     <style>
                       body {
                         margin: 0;
                         padding: 0;
                         height: 100vh;
                         background: linear-gradient(135deg, #f8f9fa, #e9ecef);
                         font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                         display: flex;
                         justify-content: center;
                         align-items: center;
                       }
            
                       .container {
                         background: #ffffff;
                         padding: 40px 30px;
                         border-radius: 12px;
                         box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
                         text-align: center;
                         max-width: 400px;
                         width: 90%;
                       }
            
                       .container h1 {
                         color: #e74c3c;
                         margin-bottom: 20px;
                         font-size: 28px;
                       }
            
                       .container p {
                         color: #555;
                         font-size: 16px;
                         margin-bottom: 15px;
                         line-height: 1.5;
                       }
            
                       .container a {
                         display: inline-block;
                         margin-top: 20px;
                         padding: 10px 20px;
                         background-color: #3498db;
                         color: #fff;
                         text-decoration: none;
                         border-radius: 8px;
                         transition: background-color 0.3s ease;
                       }
            
                       .container a:hover {
                         background-color: #2980b9;
                       }
                     </style>
                   </head>
                   <body>
            
                     <div class="container">
                       <h1>Application Not Running</h1>
                       <p>It seems the application you're trying to access is currently not running.</p>
                       <p>Please check the application status or contact support if the issue persists.</p>
                       <a href="https://tarmoqchi.uz">Go to Tarmoqchi</a>
                     </div>
            
                   </body>
                   </html>
            """;

    public static String TUNNEL_NOT_FOUND_HTML = """
            <!DOCTYPE html>
                   <html lang="en">
                   <head>
                     <meta charset="UTF-8">
                     <meta name="viewport" content="width=device-width, initial-scale=1.0">
                     <title>Tunnel Not Found</title>
                     <style>
                       body {
                         margin: 0;
                         padding: 0;
                         height: 100vh;
                         background: linear-gradient(135deg, #f8f9fa, #e9ecef);
                         font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                         display: flex;
                         justify-content: center;
                         align-items: center;
                       }
            
                       .container {
                         background: #ffffff;
                         padding: 40px 30px;
                         border-radius: 12px;
                         box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
                         text-align: center;
                         max-width: 400px;
                         width: 90%;
                         animation: fadeIn 0.8s ease-in-out;
                       }
            
                       @keyframes fadeIn {
                         from { opacity: 0; transform: translateY(-20px); }
                         to { opacity: 1; transform: translateY(0); }
                       }
            
                       .container h1 {
                         color: #e74c3c;
                         margin-bottom: 20px;
                         font-size: 28px;
                       }
            
                       .container p {
                         color: #555;
                         font-size: 16px;
                         margin-bottom: 15px;
                         line-height: 1.5;
                       }
            
                       .container a {
                         display: inline-block;
                         margin-top: 20px;
                         padding: 10px 20px;
                         background-color: #3498db;
                         color: #fff;
                         text-decoration: none;
                         border-radius: 8px;
                         transition: background-color 0.3s ease;
                       }
            
                       .container a:hover {
                         background-color: #2980b9;
                       }
                     </style>
                   </head>
                   <body>
            
                     <div class="container">
                       <h1>Tunnel Not Found</h1>
                       <p>It seems the tunnel you're trying to access does not exist or is currently unavailable.</p>
                       <p>Please verify the tunnel subdomain or contact support for assistance.</p>
                       <a href="https://tarmoqchi.uz">Go to Tarmoqchi</a>
                     </div>
            
                   </body>
                   </html>
            """;
}
