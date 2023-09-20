package plugins.fmp.multicafe2.tools;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JToolBar;

import icy.canvas.Canvas2D;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;



public class KymosCanvas2D extends Canvas2D
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8827595503996677250L;
	final JButton nextButton 				= new JButton("NEXT");
    final JButton previousButton 			= new JButton("PREVIOUS");
    final JButton zoomImageButton			= new JButton("Zoom 1:1");
    final JButton shrinkImageButton			= new JButton("Fit all");

    
    public KymosCanvas2D(Viewer viewer)
    {
        super(viewer);
    }
    
    @Override
    public void customizeToolbar(JToolBar toolBar)
    {
        super.customizeToolbar(toolBar);
        toolBar.add(previousButton);
        toolBar.add(nextButton);
        toolBar.add(zoomImageButton);
        toolBar.add(shrinkImageButton);
        
        previousButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                setPositionT( getPositionT()-1);
                
            }
        });
        
        
        nextButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	 setPositionT( getPositionT()+1);
            }
        });
        
        zoomImageButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	zoomImage_1_1();
            }
        });
        
        shrinkImageButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	shrinkImage_to_fit() ;
            }
        });

    }   	        
    
    void zoomImage_1_1() 
	{
		Sequence seqKymograph = getSequence();
		Rectangle rectImage = seqKymograph.getBounds2D();
		Rectangle rectCanvas = getCanvasVisibleRect();
		
		double scaleY = rectCanvas.getHeight() / rectImage.getHeight();;  
		double scaleX = scaleY; 
		int offsetX = (int) (rectCanvas.width / scaleX / 2); 
		setMouseImagePos(offsetX, rectImage.height  / 2);
		setScale(scaleX, scaleY, true, true);
	}
    
    void shrinkImage_to_fit() 
	{
		Sequence seqKymograph = getSequence();
		Rectangle rectImage = seqKymograph.getBounds2D();
		Rectangle rectCanvas = getCanvasVisibleRect();
		
		double scaleX = rectCanvas.getWidth() / rectImage.getWidth(); 
		double scaleY = rectCanvas.getHeight() / rectImage.getHeight();
		setMouseImagePos(rectImage.width/2, rectImage.height/ 2);
		setScale(scaleX, scaleY, true, true);
	}
}
