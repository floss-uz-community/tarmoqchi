package uz.server.config;

public class Settings {
    public static String NOT_RUNNING_APP_OF_CLIENT_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Not Running App</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background-color: #f4f4f4;
                        color: #333;
                        text-align: center;
                        padding: 50px;
                    }
                    h1 {
                        color: #e74c3c;
                    }
                    p {
                        font-size: 18px;
                    }
                </style>
            </head>
            <body>
                <h1>Application Not Running</h1>
                <p>It seems that the application you are trying to access is not running.</p>
                <p>Please check the application status or contact support.</p>
                <p>Thank you for your understanding.</p>
                <p><a href="https://tarmoqchi.uz">Go to Tarmoqchi</a></p>
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
                        font-family: Arial, sans-serif;
                        background-color: #f4f4f4;
                        color: #333;
                        text-align: center;
                        padding: 50px;
                    }
                    h1 {
                        color: #e74c3c;
                    }
                    p {
                        font-size: 18px;
                    }
                </style>
            </head>
            <body>
                <h1>Tunnel Not Found</h1>
                <p>It seems that the tunnel you are trying to access does not exist.</p>
                <p>Please check the tunnel ID or contact support.</p>
                <p>Thank you for your understanding.</p>
                <p><a href="https://tarmoqchi.uz">Go to Tarmoqchi</a></p>
            </body>
            </html>
            """;
}
