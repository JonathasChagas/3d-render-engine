import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DemoViewer
{
    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout((new BorderLayout()));

        //Slider horizontal rotation
        JSlider headingSlider = new JSlider(0,360,180);
        pane.add(headingSlider, BorderLayout.SOUTH);

        //Slider vertical rotation
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        //Render results pane
        JPanel renderPanel = new JPanel()
        {
            public void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0,0, getWidth(), getHeight());

                List<Triangle> tris = new ArrayList<>();

                 tris.add(new Triangle(new Vertex (100, 100, 100, 1),
                                        new Vertex(-100, -100, 100, 1),
                                        new Vertex(-100, 100, -100, 1),
                                        Color.WHITE));

                 tris.add(new Triangle(new Vertex (100, 100, 100, 1),
                                        new Vertex(-100, -100, 100, 1),
                                        new Vertex(100, -100, -100, 1),
                                        Color.RED));

                 tris.add(new Triangle(new Vertex (-100, 100, -100, 1),
                                        new Vertex(100, -100, -100, 1),
                                        new Vertex(100, 100, 100, 1),
                                        Color.GREEN));

                 tris.add(new Triangle(new Vertex (-100, 100, -100, 1),
                                        new Vertex(100, -100, -100, 1),
                                        new Vertex(-100, -100, 100, 1),
                                        Color.BLUE));

                double heading = Math.toRadians(headingSlider.getValue());

                Matrix3 headingTransform = new Matrix3(new double[]
                         {
                                 Math.cos(heading), 0, -Math.sin(heading),
                                 0, 1, 0,
                                 Math.sin(heading), 0, Math.cos(heading)
                         });

                double pitch = Math.toRadians(pitchSlider.getValue());

                Matrix3 pitchTransform = new Matrix3(new double[]
                        {
                                1, 0, 0,
                                0, Math.cos(pitch), Math.sin(pitch),
                                0, -Math.sin(pitch), Math.cos(pitch)
                        });

                Matrix3 transform = headingTransform.multiply(pitchTransform);

                BufferedImage img =
                        new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                double[] zBuffer = new double[img.getWidth() * img.getHeight()];

                Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);

                for (Triangle t : tris)
                {
                    Vertex v1 = transform.transform(t.v1);
                    Vertex v2 = transform.transform(t.v2);
                    Vertex v3 = transform.transform(t.v3);

                    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z, v2.w - v1.w);
                    Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z, v3.w - v1.w);

                    Vertex norm = new Vertex(
                            ab.y * ac.z - ab.z * ac.y,
                            ab.z * ac.x - ab.x * ac.z,
                            ab.x * ac.y - ab.y * ac.x,
                            1
                    );

                    double normalLength =
                            Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);

                    norm.x /= normalLength;
                    norm.y /= normalLength;
                    norm.z /= normalLength;

                    double angleCos = Math.abs(norm.z);

                    v1.x += (double) getWidth() / 2;
                    v1.y += (double) getHeight() / 2;
                    v2.x += (double) getWidth() / 2;
                    v2.y += (double) getHeight() / 2;
                    v3.x += (double) getWidth() / 2;
                    v3.y += (double) getHeight() / 2;

                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                    int maxX = (int) Math.min(img.getWidth() - 1,
                                     Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));

                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                    int maxY = (int) Math.min(img.getWidth() - 1,
                            Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

                    double triangleArea =
                            (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

                    for (int y = minY; y < maxY; y++)
                    {
                        for (int x = minX; x < maxX; x++)
                        {
                            double b1 =
                                    ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                            double b2 =
                                    ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                            double b3 =
                                    ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;

                            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1)
                            {
                                double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;

                                int zIndex = y * img.getWidth() + x;

                                if (zBuffer[zIndex] < depth)
                                {
                                    img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
                                    zBuffer[zIndex] = depth;
                                }
                            }
                        }
                    }

                    g2.drawImage(img, 0, 0, null);
                }
            }
        };


        pane.add(renderPanel, BorderLayout.CENTER);

        headingSlider.addChangeListener(e -> renderPanel.repaint());
        pitchSlider.addChangeListener(e -> renderPanel.repaint());

        frame.setTitle("3D Render Engine");
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }

    public static Color getShade (Color color, double shade)
    {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLinear, 1/2.4);
        int green = (int) Math.pow(greenLinear, 1/2.4);
        int blue = (int) Math.pow(blueLinear, 1/2.4);

        return new Color(red, green, blue);
    }

}

class Vertex
{
    double x;
    double y;
    double z;
    double w;

    Vertex(double x, double y, double z, double w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
}

class Triangle
{
    Vertex v1;
    Vertex v2;
    Vertex v3;

    Color color;

    Triangle(Vertex v1, Vertex v2, Vertex v3, Color color)
    {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
    }
}

class Matrix3
{
    double[] values;

    public Matrix3(double[] values)
    {
        this.values = values;
    }

    Matrix3 multiply(Matrix3 other)
    {
        double[] result = new double[9];
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 3; col++)
            {
                for (int i = 0; i < 3; i++)
                {
                    result[row * 3 + col] += this.values[row * 3 + i] * other.values[i * 3 + col];
                }
            }
        }
        return new Matrix3(result);
    }

    Vertex transform(Vertex in)
    {
        return new Vertex(
                in.x * values[0] + in.y * values[3] + in.z * values[6],
                in.x * values[1] + in.y * values[4] + in.z * values[7],
                in.x * values[2] + in.y * values[5] + in.z * values[8],
                1
        );
    }
}
