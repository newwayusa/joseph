import java.awt.*;

public class MouseMover {
    public static void main(String[] args) {
        try {
            // Create an instance of the Robot class
            Robot robot = new Robot();

            // Get screen dimensions
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = screenSize.width;
            int screenHeight = screenSize.height;

            // Calculate the center of the screen
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;

            // Side length of the square
            int squareSize = 100; // 100 pixels

            while (true) {
                // Move mouse in a square pattern

                // Top-left corner
                robot.mouseMove(centerX - squareSize / 2, centerY - squareSize / 2);
                Thread.sleep(1000);

                // Top-right corner
                robot.mouseMove(centerX + squareSize / 2, centerY - squareSize / 2);
                Thread.sleep(1000);

                // Bottom-right corner
                robot.mouseMove(centerX + squareSize / 2, centerY + squareSize / 2);
                Thread.sleep(1000);

                // Bottom-left corner
                robot.mouseMove(centerX - squareSize / 2, centerY + squareSize / 2);
                Thread.sleep(1000);

                // Return to the top-left corner
                robot.mouseMove(centerX - squareSize / 2, centerY - squareSize / 2);

                // Wait 5 seconds before repeating
                Thread.sleep(5000);
            }
        } catch (AWTException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
