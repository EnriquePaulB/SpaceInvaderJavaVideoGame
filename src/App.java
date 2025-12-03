import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Space Invaders");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            gamePanel.startGame();
        });
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Window size
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    // Player
    private int playerWidth = 60;
    private int playerHeight = 20;
    private int playerX;
    private int playerY;
    private int playerSpeed = 8;

    // Player movement flags (for smooth movement)
    private boolean movingLeft = false;
    private boolean movingRight = false;

    // Bullet
    private boolean bulletActive = false;
    private int bulletX;
    private int bulletY;
    private int bulletSpeed = 12;
    private int bulletWidth = 4;
    private int bulletHeight = 12;

    // Enemies grid
    private int rows = 3;
    private int cols = 8;
    private int enemyWidth = 50;
    private int enemyHeight = 20;
    private int enemySpacingX = 20;
    private int enemySpacingY = 20;
    private int enemyStartX = 80;
    private int enemyStartY = 60;
    private boolean[][] enemyAlive;

    // Enemy movement
    private int enemyOffsetX = 0;  
    private int enemyDirection = 1; 
    private int enemySpeed = 2;     

    // Score
    private int score = 0;

    // Lives
    private int lives = 3;

    // Game state
    private boolean gameOver = false;
    private boolean gameStarted = false;

    // Levels and difficulty
    // Level 0 = tutorial (easy, static enemies)
    private boolean tutorialMode = true;
    private int level = 0;          
    private int maxLevel = 5;
    private boolean playerWon = false;  

    // Game loop
    private Timer timer;

    // Starfield background
    private static final int STAR_COUNT = 80;
    private int[] starX = new int[STAR_COUNT];
    private int[] starY = new int[STAR_COUNT];
    private int[] starSpeed = new int[STAR_COUNT];
    private Random random = new Random();

    // Explosions
    private static final int MAX_EXPLOSIONS = 20;
    private int[] explosionX = new int[MAX_EXPLOSIONS];
    private int[] explosionY = new int[MAX_EXPLOSIONS];
    private int[] explosionTimer = new int[MAX_EXPLOSIONS];

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(this);

        // Player starts at bottom center
        playerX = WIDTH / 2 - playerWidth / 2;
        playerY = HEIGHT - 80;

        // Initialize starfield
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = random.nextInt(WIDTH);
            starY[i] = random.nextInt(HEIGHT);
            starSpeed[i] = 1 + random.nextInt(3); // speed 1–3
        }

        // Enemies array
        enemyAlive = new boolean[rows][cols];
        // Start at level 0 (tutorial)
        initLevel(0);

        // Timer: ~60 FPS (16 ms per tick)
        timer = new Timer(16, this);
    }

    public void startGame() {
        timer.start();
    }

    // Initialize a given level with symmetric enemy pattern
    private void initLevel(int lvl) {
        level = lvl;

        // Level 0 = tutorial
        tutorialMode = (level == 0);

        // Reset horizontal offset
        enemyOffsetX = 0;

        // Configure movement based on level
        if (tutorialMode) {
            enemyDirection = 0;  
            enemySpeed = 0;
        } else {
            enemyDirection = 1;          
            enemySpeed = 1 + level;       
        }

        // Clear all enemies
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                enemyAlive[r][c] = false;
            }
        }

        int rowsActive;
        int colsActive;

        if (level == 0) {          
            rowsActive = 1;        
            colsActive = 4;        
        } else if (level == 1) {   
            rowsActive = 1;
            colsActive = 6;        
        } else if (level == 2) {  
            rowsActive = 2;
            colsActive = 6;        
        } else if (level == 3) {   
            rowsActive = 2;
            colsActive = 8;        
        } else if (level == 4) {  
            rowsActive = 3;
            colsActive = 6;        
        } else {                  
            rowsActive = 3;
            colsActive = 8;       
        }

        // Clamp in case something exceeds our grid
        if (rowsActive > rows) rowsActive = rows;
        if (colsActive > cols) colsActive = cols;

        // Center horizontally: start column so that block is symmetric
        int startCol = (cols - colsActive) / 2;

        // Activate enemies in a centered rectangle
        for (int r = 0; r < rowsActive; r++) {
            for (int c = 0; c < colsActive; c++) {
                int colIndex = startCol + c;
                if (colIndex >= 0 && colIndex < cols) {
                    enemyAlive[r][colIndex] = true;
                }
            }
        }
    }

    // Reset entire game when player presses Enter after game over / win
    private void resetGame() {
        score = 0;
        lives = 3;
        gameOver = false;
        playerWon = false;
        gameStarted = true;

        // Reset player position
        playerX = WIDTH / 2 - playerWidth / 2;
        playerY = HEIGHT - 80;

        // Reset bullet
        bulletActive = false;

        // Restart from tutorial (level 0)
        initLevel(0);

        // Restart the game loop
        if (!timer.isRunning()) {
            timer.start();
        }

        // Redraw
        repaint();
    }

    // ------------ UPDATE METHODS ------------

    private void updatePlayer() {
        if (movingLeft) {
            playerX -= playerSpeed;
            if (playerX < 0) {
                playerX = 0;
            }
        }

        if (movingRight) {
            playerX += playerSpeed;
            if (playerX + playerWidth > WIDTH) {
                playerX = WIDTH - playerWidth;
            }
        }
    }

    private void updateBullet() {
        if (!bulletActive || gameOver) return;

        bulletY -= bulletSpeed;

        // If bullet goes off screen (missed shot)
        if (bulletY + bulletHeight < 0) {
            bulletActive = false;
            lives--;

            if (lives <= 0) {
                lives = 0;
                gameOver = true;
                playerWon = false;
                timer.stop(); 
            }
        }
    }

        private void updateStars() {
        for (int i = 0; i < STAR_COUNT; i++) {
            starY[i] += starSpeed[i];

            // If star goes off bottom, respawn at top with new position/speed
            if (starY[i] > HEIGHT) {
                starX[i] = random.nextInt(WIDTH);
                starY[i] = 0;
                starSpeed[i] = 1 + random.nextInt(3);
            }
        }
    }


    private void updateEnemiesMovement() {
        // No movement in tutorial mode
        if (tutorialMode) {
            return;
        }

        // Move the whole group horizontally
        enemyOffsetX += enemySpeed * enemyDirection;

        // Compute leftmost and rightmost positions of the group
        int leftMost = enemyStartX + enemyOffsetX;
        int rightMost = enemyStartX + enemyOffsetX
                + (cols - 1) * (enemyWidth + enemySpacingX)
                + enemyWidth;

        if (leftMost < 20 || rightMost > WIDTH - 20) {
            enemyDirection *= -1; 
        }
    }

    private void checkCollisions() {
        if (!bulletActive) return;

        Rectangle bulletRect = new Rectangle(bulletX, bulletY, bulletWidth, bulletHeight);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!enemyAlive[r][c]) continue;

                int ex = enemyStartX + enemyOffsetX + c * (enemyWidth + enemySpacingX);
                int ey = enemyStartY + r * (enemyHeight + enemySpacingY);
                Rectangle enemyRect = new Rectangle(ex, ey, enemyWidth, enemyHeight);

                if (bulletRect.intersects(enemyRect)) {
                    enemyAlive[r][c] = false;
                    bulletActive = false;
                    score += 10; 
                    int centerX = ex + enemyWidth / 2;
                    int centerY = ey + enemyHeight / 2;
                    spawnExplosion(centerX, centerY);

                    return; 
                }
            }
        }
    }

    private boolean areAllEnemiesDead() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (enemyAlive[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    private void spawnExplosion(int x, int y) {
        for (int i = 0; i < MAX_EXPLOSIONS; i++) {
            if (explosionTimer[i] <= 0) { // free slot
                explosionX[i] = x;
                explosionY[i] = y;
                explosionTimer[i] = 20;   // lifetime in frames (~0.3 sec at 60 FPS)
                break;
            }
        }
    }

    private void updateExplosions() {
        for (int i = 0; i < MAX_EXPLOSIONS; i++) {
            if (explosionTimer[i] > 0) {
                explosionTimer[i]--;
            }
        }
    }

    // ------------ GAME LOOP ------------

     @Override
    public void actionPerformed(ActionEvent e) {
        updateStars();
        updateExplosions();

        if (!gameStarted || gameOver) {
            repaint();
            return;
        }

        updatePlayer();
        updateBullet();
        updateEnemiesMovement();
        checkCollisions();

        // If all enemies are gone, go to the next level or win
        if (areAllEnemiesDead()) {
            if (level < maxLevel) {
                initLevel(level + 1); 
            } else {
                gameOver = true;
                playerWon = true;
                timer.stop();
            }
        }

        repaint();
    }

    // ------------ RENDERING ------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Enable better graphics
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw starfield background
        g2.setColor(Color.WHITE);
        for (int i = 0; i < STAR_COUNT; i++) {
            g2.fillRect(starX[i], starY[i], 2, 2); // small white star
        }

        // START SCREEN
        if (!gameStarted) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            String title = "Space Invaders";
            int titleWidth = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, (WIDTH - titleWidth) / 2, HEIGHT / 2 - 40);

            g2.setFont(new Font("Arial", Font.PLAIN, 24));
            String msg = "Press Enter to start";
            int msgWidth = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (WIDTH - msgWidth) / 2, HEIGHT / 2 + 10);

            return; 
        }

        // HUD: Score, Lives, Level
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("Score: " + score, 20, 30);

        g2.drawString("Lives: " + lives, WIDTH - 140, 30);

        String levelText = "Level: " + level;
        int levelWidth = g2.getFontMetrics().stringWidth(levelText);
        g2.drawString(levelText, (WIDTH - levelWidth) / 2, 30);

        // Draw player
        g2.setColor(Color.GREEN);
        g2.fillRect(playerX, playerY, playerWidth, playerHeight);

        // Draw bullet
        if (bulletActive) {
            g2.setColor(Color.YELLOW);
            g2.fillRect(bulletX, bulletY, bulletWidth, bulletHeight);
        }

        // Draw enemies
        g2.setColor(Color.RED);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (enemyAlive[r][c]) {
                    int ex = enemyStartX + enemyOffsetX + c * (enemyWidth + enemySpacingX);
                    int ey = enemyStartY + r * (enemyHeight + enemySpacingY);
                    g2.fillRect(ex, ey, enemyWidth, enemyHeight);
                }
            }
        }

        // Draw explosions
        g2.setColor(Color.ORANGE);
        for (int i = 0; i < MAX_EXPLOSIONS; i++) {
            if (explosionTimer[i] > 0) {
                int life = explosionTimer[i];
                int maxLife = 20;
                int radius = 5 + (maxLife - life);

                int x = explosionX[i] - radius;
                int y = explosionY[i] - radius;
                int size = radius * 2;

                g2.fillOval(x, y, size, size);
            }
        }

        // If game over, draw message + restart hint
        if (gameOver) {
            g2.setFont(new Font("Arial", Font.BOLD, 40));

            String msg = playerWon ? "YOU WIN!" : "GAME OVER";
            int msgWidth = g2.getFontMetrics().stringWidth(msg);
            int msgX = (WIDTH - msgWidth) / 2;
            int msgY = HEIGHT / 2;
            g2.drawString(msg, msgX, msgY);

            // Smaller "Press Enter to play again" text
            g2.setFont(new Font("Arial", Font.PLAIN, 24));
            String subMsg = "Press Enter to play again";
            int subWidth = g2.getFontMetrics().stringWidth(subMsg);
            int subX = (WIDTH - subWidth) / 2;
            int subY = msgY + 40;
            g2.drawString(subMsg, subX, subY);
        }
    }

    // ------------ INPUT ------------

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // If the game hasn't started yet, pressing Enter starts it
        if (!gameStarted) {
            if (key == KeyEvent.VK_ENTER) {
                gameStarted = true;
            }
            return;
        }

        // If game is over, allow restart with Enter
        if (gameOver) {
            if (key == KeyEvent.VK_ENTER) {
                resetGame();
            }
            return;
        }

        // Movement flags
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            movingLeft = true;
        }

        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            movingRight = true;
        }

        // Shoot
        if (key == KeyEvent.VK_SPACE) {
            shoot();
        }
    }

    private void shoot() {
        if (bulletActive) return;

        bulletActive = true;
        bulletX = playerX + playerWidth / 2 - bulletWidth / 2;
        bulletY = playerY - bulletHeight;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            movingLeft = false;
        }

        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            movingRight = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}
