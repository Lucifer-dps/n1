import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Color;
// Enum for Room Types
enum RoomType {
    BEDROOM, BATHROOM, KITCHEN, LIVINGROOM
}
// Room Class
class Room implements Serializable {

    int x, y, width, height;
    RoomType type;
    Color color;
    static final int wallThickness = 2; // Default wall thickness
    public Room(int x, int y, int width, int height, RoomType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.color = getColorForType(type);
    }
    
    private Color getColorForType(RoomType type) {
        return switch (type) {
            case BEDROOM -> Color.GREEN;
            case BATHROOM -> Color.BLUE;
            case KITCHEN -> Color.RED;
            case LIVINGROOM -> Color.ORANGE;
        };
    }
    public Rectangle getBounds() {
        return new Rectangle(x - wallThickness, y - wallThickness,
                width + 2 * wallThickness, height + 2 * wallThickness);
    }
    public boolean overlaps(Rectangle other) {
        return getBounds().intersects(other);
    }
    public void keepWithinBounds(int canvasWidth, int canvasHeight, ArrayList<Room> existingRooms) {
        x = Math.max(wallThickness, Math.min(x, canvasWidth - width - wallThickness));
        y = Math.max(wallThickness, Math.min(y, canvasHeight - height - wallThickness));
        for (Room existingRoom : existingRooms) {
            if (existingRoom != this && existingRoom.overlaps(getBounds())) {
                // If there's an overlap, push the room back inside the canvas bounds
                x = Math.max(Room.wallThickness, Math.min(x, canvasWidth - width - Room.wallThickness));
                y = Math.max(Room.wallThickness, Math.min(y, canvasHeight - height - Room.wallThickness));
            }
        }
    }
}
// Furniture Class with rotation (used for both Door and Window)
class Furniture implements Serializable {
    int x, y, width, height;
    String type;
    ImageIcon icon;
    String iconPath;
     private static final int RESIZE_HANDLE_SIZE = 8;  // Size of the resize handle (in pixels)

    public Furniture(int x, int y, int width, int height, String type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        
        String iconPath = switch (type)
         {
            case "Sofa" -> "/sofa.png";
            case "Table" -> "/table.png";
            case "Chair" -> "/chair.png";
            case "Bed" -> "/bed.png";
            case "Dining_Set" -> "/diningset.png";
            case "Door" -> "/door.png";
            case "Window" -> "/window.png";
             case "Stove" -> "/stove.png";
              case "Shower" -> "/shower.png";
               case "Commode" -> "/commode.png";
                case "Wash_Basin" -> "/washbasin.png";
                 case "Sink" -> "/sink.png";

            

            default -> "/default.png";
        };
        this.icon = new ImageIcon(getClass().getResource(iconPath)); // Using the set iconPath here

        Image img = icon.getImage();
        Image scaledImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        icon = new ImageIcon(scaledImage);
    }
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    public void resize(int dx, int dy) {
        this.width += dx;
        this.height += dy;
    }

    // Determine if a point is inside the resizing handle
    public boolean isNearResizeHandle(Point point) {
        // Check the right corner
        return (point.x >= x + width - RESIZE_HANDLE_SIZE && point.x <= x + width &&
                point.y >= y + height - RESIZE_HANDLE_SIZE && point.y <= y + height);
    }

    // Draw the resize handles (small squares in the corners)
    public void drawResizeHandles(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(x + width - RESIZE_HANDLE_SIZE, y + height - RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);  // Bottom-right corner
    }
    public void keepWithinBounds(int canvasWidth, int canvasHeight) {
        x = Math.max(0, Math.min(x, canvasWidth - width));
        y = Math.max(0, Math.min(y, canvasHeight - height));

       
    }

    // Rotate furniture by 90 degrees (swap width and height)
    public void rotate() {
        int temp = width;
        width = height;
        height = temp;
    }
}
// Door class (a type of Furniture)
class Door extends Furniture {
    public Door(int x, int y, int width, int height) {
        super(x, y, width, height, "Door");
    }
    @Override
    public void keepWithinBounds(int canvasWidth, int canvasHeight) {
        // Ensure the door stays within the walls
        super.keepWithinBounds(canvasWidth, canvasHeight);
    }
}
// Window class (a type of Furniture)
class Window extends Furniture {
    public Window(int x, int y, int width, int height) {
        super(x, y, width, height, "Window");
    }
    @Override
    public void keepWithinBounds(int canvasWidth, int canvasHeight) {
        super.keepWithinBounds(canvasWidth, canvasHeight);
    }
}
// Canvas Panel
class CanvasPanel extends JPanel {
    private ArrayList<Room> rooms = new ArrayList<>();
    private ArrayList<Furniture> furnitureList = new ArrayList<>();
    private Room selectedRoom = null;
    private Furniture selectedFurniture = null;
    private Point initialClick;
    private Furniture resizingFurniture = null;
    private Point resizingInitialClick;
    private boolean isResizing = false;


    private static final int ROOM_SPACING = 20; // Space between rooms horizontally
    private static final int ROOM_HEIGHT = 100; // Height of each room
    private static final int CANVAS_WIDTH = 900; // Canvas width
    private static final int CANVAS_HEIGHT = 600; // Canvas height
    private static final int MAX_ROOMS_IN_ROW = 1000; // Maximum rooms per row
    public static final int GRID_SIZE = 20; // Grid size
    public CanvasPanel() {
        setBackground(Color.LIGHT_GRAY);
        setPreferredSize(new Dimension(CANVAS_WIDTH/4, CANVAS_HEIGHT));
        // Mouse listeners for rooms and furniture
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                for (Furniture furniture : furnitureList) {
                    if (furniture.isNearResizeHandle(e.getPoint())) {
                        resizingFurniture = furniture;
                        resizingInitialClick = e.getPoint();
                        isResizing = true;
                        return;
                    }
                }
                selectRoom(e.getPoint());
                selectFurniture(e.getPoint());
                initialClick = e.getPoint();
            }
            public void mouseReleased(MouseEvent e) {
                isResizing = false;
                if (resizingFurniture != null) {
                    resizingFurniture.keepWithinBounds(getWidth(), getHeight());
                    repaint();
                }
                if (selectedRoom != null) {
                    selectedRoom.keepWithinBounds(getWidth(), getHeight(), rooms);
                    repaint();
                }
                if (selectedFurniture != null) {
                    selectedFurniture.keepWithinBounds(getWidth(), getHeight());
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
    public void mouseDragged(MouseEvent e) {
        // Handle resizing of furniture
        if (isResizing && resizingFurniture != null) {
            // Calculate the change in mouse position for resizing
            int dx = e.getX() - resizingInitialClick.x;
            int dy = e.getY() - resizingInitialClick.y;

            // Resize the furniture based on the mouse movement
            resizingFurniture.resize(dx, dy);
            resizingInitialClick = e.getPoint(); // Update the initial click position for next drag
            repaint();
            return; // Stop further processing as resizing is happening
        }

        // Handle furniture dragging (when no resizing)
        if (selectedFurniture != null && selectedRoom != null) {
            // Calculate the new position for the furniture
            int dx = e.getX() - initialClick.x;
            int dy = e.getY() - initialClick.y;

            // Move the furniture inside the selected room, if applicable
            selectedFurniture.x += dx;
            selectedFurniture.y += dy;

            if (selectedRoom != null) {
                moveFurnitureInsideRoom(selectedFurniture, selectedRoom);
            }

            // Update the initial position for next drag event
            initialClick = e.getPoint();
            repaint();
        }

        // Handle room dragging
        if (selectedRoom != null) {
            // Calculate the new position for the room itself
            int dx = e.getX() - initialClick.x;
            int dy = e.getY() - initialClick.y;
            selectedRoom.x += dx;
            selectedRoom.y += dy;
            initialClick = e.getPoint();

            // Check if the room overlaps with other rooms
            boolean overlapDetected = false;
            for (Room room : rooms) {
                if (room != selectedRoom && room.overlaps(selectedRoom.getBounds())) {
                    overlapDetected = true;
                    break; // If overlap is detected, stop checking further
                }
            }

            // If overlap is detected, revert position and show error message
            if (overlapDetected) {
                selectedRoom.x -= dx;
                selectedRoom.y -= dy;
                JOptionPane.showMessageDialog(CanvasPanel.this, "Cannot move the room: Overlap detected!");
            } else {
                // Ensure the room stays within canvas bounds
                selectedRoom.keepWithinBounds(getWidth(), getHeight(), rooms);
                repaint();
            }
        }

        // Handle furniture dragging freely on the canvas
        if (selectedFurniture != null) {
            // Calculate the new position for the selected furniture
            int dx = e.getX() - initialClick.x;
            int dy = e.getY() - initialClick.y;
            selectedFurniture.x += dx;
            selectedFurniture.y += dy;
            initialClick = e.getPoint();

            // Check if the furniture overlaps with other furniture
            boolean overlapDetected = false;
            for (Furniture furniture : furnitureList) {
                if (furniture != selectedFurniture && furniture.getBounds().intersects(selectedFurniture.getBounds())) {
                    overlapDetected = true;
                    break; // If overlap is detected, stop checking further
                }
            }

            // If overlap is detected, revert position and show error message
            if (overlapDetected) {
                selectedFurniture.x -= dx;
                selectedFurniture.y -= dy;
                JOptionPane.showMessageDialog(CanvasPanel.this, "No overlap between furniture!");
            }

            repaint();
        }
    }
});
    }
    public void moveFurnitureInsideRoom(Furniture furniture, Room room) {
        // Check if the furniture is moving within the bounds of the room
        int maxX = room.x + room.width - furniture.width;
        int maxY = room.y + room.height - furniture.height;

        // Update the position of the furniture with these limits
        furniture.x = Math.max(room.x, Math.min(furniture.x, maxX));
        furniture.y = Math.max(room.y, Math.min(furniture.y, maxY));
    }
    // Add rotation functionality
    public void rotateSelectedFurniture() {
        if (selectedFurniture != null) {
            selectedFurniture.rotate();
            selectedFurniture.keepWithinBounds(getWidth(), getHeight());
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "No furniture selected to rotate.");
        }
    }
    public void addRoom(Room room) {
        room.keepWithinBounds(getWidth(), getHeight(), rooms); // Make sure it's within bounds and not overlapping   
        // Check for overlap with existing rooms
        for (Room existingRoom : rooms) {
            if (existingRoom.overlaps(room.getBounds())) {
                JOptionPane.showMessageDialog(this, "Rooms cannot overlap!");
                return; // Prevent adding the room
            }
        }
        rooms.add(room);
        repaint();
    }
    public void addFurniture(Furniture furniture) {
        // Check for overlap with existing furniture before adding
        for (Furniture existingFurniture : furnitureList) {
            if (existingFurniture.getBounds().intersects(furniture.getBounds())) {
                JOptionPane.showMessageDialog(this, "Furniture cannot overlap!");
                return;
            }
        }
        // Keep the furniture within bounds and snap to grid
        furniture.keepWithinBounds(getWidth(), getHeight());
        furnitureList.add(furniture);
        repaint();
    }
    public void deleteSelectedRoom() {
        if (selectedRoom != null) {
            rooms.remove(selectedRoom);
            selectedRoom = null;
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "No room selected to delete.");
        }
    }
    public void deleteSelectedFurniture() {
        if (selectedFurniture != null) {
            furnitureList.remove(selectedFurniture);
            selectedFurniture = null;
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "No furniture selected to delete.");
        }
    }
    public void savePlan(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(rooms);
            oos.writeObject(furnitureList); // Save furniture as well
            JOptionPane.showMessageDialog(this, "Plan saved successfully!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save the plan.");
        }
    }
    @SuppressWarnings("unchecked")
    public void loadPlan(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            rooms = (ArrayList<Room>) ois.readObject();
            furnitureList = (ArrayList<Furniture>) ois.readObject();
            repaint();
            JOptionPane.showMessageDialog(this, "Plan loaded successfully!");
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Failed to load the plan.");
        }
    }
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw rooms with walls
        for (Room room : rooms) {
            // Draw wall (thick outline)
            g.setColor(Color.DARK_GRAY); // Wall color
            g.fillRect(room.x - Room.wallThickness, room.y - Room.wallThickness,
                    room.width + 2 * Room.wallThickness, room.height + 2 * Room.wallThickness);
            // Draw room interior
            g.setColor(room.color);
            g.fillRect(room.x, room.y, room.width, room.height);

            // Draw room border
            g.setColor(Color.BLACK);
            g.drawRect(room.x, room.y, room.width, room.height);
        }
        // Draw furniture (including doors and windows)
        for (Furniture furniture : furnitureList) {
            if (furniture instanceof Window) {
                g.setColor(Color.CYAN); // Color for windows
                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f, new float[]{10.0f}, 0.0f));
                g2d.drawRect(furniture.x, furniture.y, furniture.width, furniture.height); // Dashed line for window
            } else if (furniture instanceof Door) {
                g.setColor(Color.YELLOW); // Color for doors (opening in wall)
                g.fillRect(furniture.x, furniture.y, furniture.width, furniture.height); // Door as opening
            } else {
                g.setColor(Color.GRAY); // Default color for other furniture
                g.fillRect(furniture.x, furniture.y, furniture.width, furniture.height);
            }
        }
        for (Furniture furniture : furnitureList) {
            g.drawImage(furniture.icon.getImage(), furniture.x, furniture.y, furniture.width, furniture.height, this);
        }
    }
    // Method to select a room when clicked
    private void selectRoom(Point point) {
        selectedRoom = null;
        for (Room room : rooms) {
            if (room.getBounds().contains(point)) {
                selectedRoom = room;
                repaint();
                return;
            }
        }
    }
    // Method to select a piece of furniture when clicked
    private void selectFurniture(Point point) {
        selectedFurniture = null;
        for (Furniture furniture : furnitureList) {
            if (furniture.getBounds().contains(point)) {
                selectedFurniture = furniture;
                repaint();
                return;
            }
        }
    }
}
// Control Panel
class ControlPanel extends JPanel {
    private CanvasPanel canvas;
    public ControlPanel(CanvasPanel canvas) {
        this.canvas = canvas;
        setLayout(new GridLayout(0, 2, 5, 5));
        JLabel widthLabel = new JLabel("Width:");
        JTextField widthField = new JTextField(6);
        JLabel heightLabel = new JLabel("Height:");
        JTextField heightField = new JTextField(6);
        JComboBox<RoomType> roomTypeCombo = new JComboBox<>(RoomType.values());
        JButton addRoomButton = new JButton("Add Room");
        addRoomButton.addActionListener(e -> {
            try {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
                Room room = new Room(10, 10, width, height, type);
                canvas.addRoom(room);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid dimensions.");
            }
        });
        JButton deleteRoomButton = new JButton("Delete Room");
        deleteRoomButton.addActionListener(e -> canvas.deleteSelectedRoom());
        // Add new furniture items here
        JLabel furnitureLabel = new JLabel("Furniture/Fixture Options:");
        JComboBox<String> furnitureCombo = new JComboBox<>(new String[]{
            "Sofa", "Table", "Chair", "Bed", "Dining_Set", "Door", "Window","Commode", "Wash_Basin", "Shower", "Sink","Stove"
        });
        JButton addFurnitureButton = new JButton("Add Furniture/Fixture");
        addFurnitureButton.addActionListener(e -> {
            String selectedFurniture = (String) furnitureCombo.getSelectedItem();
            Furniture furniture;
            String iconPath = "";  // Declare iconPath
            switch (selectedFurniture) {
                case "Sofa" -> iconPath = "/sofa.png";
                case "Table" -> iconPath = "/table.png";
                case "Chair" -> iconPath = "/chair.png";
                case "Bed" -> iconPath = "/bed.png";
                case "Dining_Set" -> iconPath = "/diningset.png"; 
                case "Door" -> iconPath = "/door.png";
                case "Window" ->  iconPath = "/window.png";
                 case "Stove" -> iconPath = "/stove.png";
                case "Shower" -> iconPath ="/shower.png";
                case "Commode" -> iconPath ="/commode.png";
                case "Wash_Basin" ->iconPath = "/washbasin.png";
                case "Sink" ->iconPath = "/sink.png";


                default -> iconPath = "/default.png";  // Default for unknown furniture
            }
            if ("Door".equals(selectedFurniture)) {
                furniture = new Door(50, 50, 50, 20); // Example dimensions for door
            } else if ("Window".equals(selectedFurniture)) {
                furniture = new Window(100, 50, 60, 20); // Example dimensions for window
            } else {
                furniture = new Furniture(50, 50, 50, 30, selectedFurniture);
            }

            furniture.iconPath = iconPath;  // Make sure to set iconPath after construction

            canvas.addFurniture(furniture);              
        });
        JButton deleteFurnitureButton = new JButton("Delete Furniture/Fixture");
        deleteFurnitureButton.addActionListener(e -> canvas.deleteSelectedFurniture());

        JButton savePlanButton = new JButton("Save Plan");
        savePlanButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                canvas.savePlan(fileChooser.getSelectedFile());
            }
        });
        JButton loadPlanButton = new JButton("Load Plan");
        loadPlanButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                canvas.loadPlan(fileChooser.getSelectedFile());
            }
        });
        // Add Rotate button for furniture
        JButton rotateButton = new JButton("Rotate Furniture");
        rotateButton.addActionListener(e -> canvas.rotateSelectedFurniture());
        add(widthLabel);
        add(widthField);
        add(heightLabel);
        add(heightField);
        add(new JLabel("Room Type:"));
        add(roomTypeCombo);
        add(addRoomButton);
        add(deleteRoomButton);
        add(new JLabel("Furniture/Fixture:"));
        add(furnitureCombo);
        add(addFurnitureButton);
        add(deleteFurnitureButton);  // Add delete furniture button
        add(savePlanButton);
        add(loadPlanButton);
        add(rotateButton);  // Add rotate button
    }
}
// Main Frame
public class FloorPlanner {
    public static void main(String[] args) {
        JFrame frame = new JFrame("2D Floor Planner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        CanvasPanel canvas = new CanvasPanel();
        ControlPanel controls = new ControlPanel(canvas);
        frame.setLayout(new BorderLayout());
        frame.add(controls, BorderLayout.WEST);
        frame.add(canvas, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}