package gx.movie;

import java.util.List;
import java.util.Random;

public class DayOn implements FrameRenderer {

	static final int[] xCoords = {11,17,21,26,31, 4, 8,12,17,21,25,29,35,40};
	static final int[] yCoords = {9,9,9,9,9,     17,17,17,17,17,17,17,17,17};
	static final int[] widths = {5,3,4,4,5,      3,3,4,3,3,3,5,4,3};	
	
	// 0,0    47,0    47,31     0,31
	static int[] startX = {47, 47, 0, 0,  31,   0,0,0,0,0,0,0,0,0};
	static int[] startY = {31,  0, 0, 31, 31,   0,0,0,0,0,0,0,0,0};
	
	static final int numFrames = 17;
	
	@Override
	public void render(LEDGrid grid) {
		
		/*
		double [] ed = calculateEllipse(22, 13, 18, 13, 360, 13);		
		
		for(int x=0;x<ed.length;x=x+2) {
			startX[13-x/2] = (int) Math.round(ed[x]);
			startY[13-x/2] = (int) Math.round(ed[x+1]);
			//System.out.println(ed[x]+" ::: "+ed[x+1]);
		}
		*/
		
		Random rand = new Random(122);
		
		for(int n=0;n<14;++n) {
			int ofsX = rand.nextInt(10);
			int ofsY = rand.nextInt(10);
			
			int subside = rand.nextInt(2);
			if(subside==1) ofsX = 47-ofsX;
			subside = rand.nextInt(2);
			if(subside==1) ofsY = 31-ofsY;
			
			
			int side = rand.nextInt(4);
			
			switch(side) {
			case 0 :				
				startX[n] = ofsX;
				startY[n] = -5;
				break;
			case 1 :
				startX[n] = 48;
				startY[n] = ofsY;
				break;
			case 2 :
				startX[n] = ofsX;
				startY[n] = 32;
				break;
			case 3 :
				startX[n] = -5;
				startY[n] = ofsY;
				break;
			}
					
		}
		
		
		for(int fn = 0; fn<numFrames;++fn) {
			for(int on=0;on<widths.length;++on) {
				int [] cor = getCoords(on,fn);
				int xc = cor[0];
				int yc = cor[1];
				for(int y=0;y<5;++y) {
					for(int x=0;x<widths[on];++x) {
						grid.setPoint(xc+x, yc+y, 
								(byte)(grid.getPoint(xCoords[on]+x, yCoords[on]+y,18) | grid.getPoint(xc+x, yc+y,fn)),
								fn);
					}
				}				
			}
		}		

	}
	
	private int [] getCoords(int object, int frame) {
		int [] ret = new int[2];
		
		if(frame==0) {
			ret[0] = startX[object];
			ret[1] = startY[object];
			return ret;
		}
		
		if(frame>=(numFrames-1)) {
			ret[0] = xCoords[object];
			ret[1] = yCoords[object];
			return ret;
		}
		
		double xr = (xCoords[object] - startX[object]); // Range to cover
		double yr = (yCoords[object] - startY[object]);
		
		double cov = ((double)frame/(double)(numFrames-1)); // How much of range is covered
		
		// Cover the range
		double x = xr*cov + startX[object];
		double y = yr*cov + startY[object];
		
		// Round
		ret[0] = (int) Math.round(x);
		ret[1] = (int) Math.round(y);
		
		return ret;
	}
	
	/*
	* This functions returns an array containing 36 points to draw an
	* ellipse.
	*
	* @param x {double} X coordinate
	* @param y {double} Y coordinate
	* @param a {double} Semimajor axis
	* @param b {double} Semiminor axis
	* @param angle {double} Angle of the ellipse
	*/
	private double [] calculateEllipse(double x, double y, double a, double b, double angle, int steps) 
	{
	  double [] ret = new double[(steps+1)*2];
	 
	  // Angle is given by Degree Value
	  double beta = -angle * (Math.PI / 180); //(Math.PI/180) converts Degree Value into Radians
	  double sinbeta = Math.sin(beta);
	  double cosbeta = Math.cos(beta);
	 
	  int datap = 0;
	  for (int i = 0; i < 360; i += 360 / steps) 
	  {
	    double alpha = i * (Math.PI / 180) ;
	    double sinalpha = Math.sin(alpha);
	    double cosalpha = Math.cos(alpha);
	 
	    double X = x + (a * cosalpha * cosbeta - b * sinalpha * sinbeta);
	    double Y = y + (a * cosalpha * sinbeta + b * sinalpha * cosbeta);
	 
	    ret[datap++] = X;
	    ret[datap++] = Y;	    
	   }
	 
	  return ret;
	}

}
