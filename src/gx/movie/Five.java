package gx.movie;

public class Five implements FrameRenderer {
	
	static int numFrames = 16;

	@Override
	public void render(LEDGrid grid) {
		
		for(int ns=0;ns<numFrames;++ns) {
			double [] ed = calculateEllipse(23,15,ns*2,ns*2,360,5);
			for(int n=0;n<ed.length;n=n+2) {
				drawCircle(ed[n],ed[n+1],ns*1.5,grid,ns);
				//grid.setPoint((int)Math.round(ed[n]), (int)Math.round(ed[n+1]), (byte)1,ns);
			}			
		}	
	}
	
	private void drawCircle(double x, double y, double r, LEDGrid grid, int frame)
	{
		if(x==0.0 && y==0.0) return;
		double [] ed = calculateEllipse(x, y, r, r, 360, 360);
		for(int n=0;n<ed.length;n=n+2) {
			grid.setPoint((int)Math.round(ed[n]), (int)Math.round(ed[n+1]), (byte)1, frame);
		}
		
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
