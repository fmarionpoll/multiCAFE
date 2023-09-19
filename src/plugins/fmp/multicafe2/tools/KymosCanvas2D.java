package plugins.fmp.multicafe2.tools;

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
	private Sequence sequence;
    final JButton nextButton = new JButton(">");
    final JButton previousButton = new JButton("<");
    
    public KymosCanvas2D(Viewer viewer)
    {
        super(viewer);
        sequence = getSequence();
    }
    
    @Override
    public void customizeToolbar(JToolBar toolBar)
    {
        super.customizeToolbar(toolBar);
        toolBar.add(previousButton);
        previousButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                if (sequence != null) 
                	sequence.getFirstImage();
            }
        });
        
        toolBar.add(nextButton);
        nextButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                if (sequence != null) 
                	sequence.getLastImage();
            }
        });

    }   	        
    
}
