package gx.movie;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MovieMaker
{
	
	public static void main(String [] args) throws Exception {
		
		OutputStream oss = new FileOutputStream(args[0]);
		
		
		
		for(int z=1;z<args.length;++z) {	
			List<byte[]> data = new ArrayList<byte[]>();
			Reader r = new FileReader(args[z]);
			BufferedReader br = new BufferedReader(r);
			while(true) {
				String g = br.readLine();				
				if(g==null) break;
				byte [] dd = new byte[48*32];
				data.add(dd);					
				for(int y=0;y<32;++y) {
					g = br.readLine();
					for(int x=0;x<48;++x) {
						if(g.charAt(x)!='.') {
							dd[y*48+x] = 1;
						}						
					}
				}
				br.readLine();
			}
			br.close();		
			saveBinary(data,oss);
		}
		
		oss.flush();
		oss.close();
	}
			
	static void saveBinary(List<byte[]> data, OutputStream oss) throws IOException
	{

		int width = 48;
		int total = 0;

		for(byte [] i : data) {
			
			int [] dat = new int[192];			
			
			// Lower right quadrant
			int pos = 0;
			for(int col=24;col<width;col=col+1) {
				for(int row=16;row<32;row=row+8) {				
					int da = 0;
					da = da | ((i[(row+0)*width+col]&1)<<0);
					da = da | ((i[(row+1)*width+col]&1)<<1);
					da = da | ((i[(row+2)*width+col]&1)<<2);
					da = da | ((i[(row+3)*width+col]&1)<<3);
					da = da | ((i[(row+4)*width+col]&1)<<4);
					da = da | ((i[(row+5)*width+col]&1)<<5);
					da = da | ((i[(row+6)*width+col]&1)<<6);
					da = da | ((i[(row+7)*width+col]&1)<<7);
					dat[pos++] = da;
				}
			}
			// Lower left quadrant
			for(int col=0;col<24;col=col+1) {
				for(int row=16;row<32;row=row+8) {				
					int da = 0;
					da = da | ((i[(row+0)*width+col]&1)<<0);
					da = da | ((i[(row+1)*width+col]&1)<<1);
					da = da | ((i[(row+2)*width+col]&1)<<2);
					da = da | ((i[(row+3)*width+col]&1)<<3);
					da = da | ((i[(row+4)*width+col]&1)<<4);
					da = da | ((i[(row+5)*width+col]&1)<<5);
					da = da | ((i[(row+6)*width+col]&1)<<6);
					da = da | ((i[(row+7)*width+col]&1)<<7);
					dat[pos++] = da;
				}
			}
			// Upper right quadrant
			for(int col=47;col>=24;col=col-1) {
				for(int row=15;row>0;row=row-8) {				
					int da = 0;
					da = da | ((i[(row-0)*width+col]&1)<<0);
					da = da | ((i[(row-1)*width+col]&1)<<1);
					da = da | ((i[(row-2)*width+col]&1)<<2);
					da = da | ((i[(row-3)*width+col]&1)<<3);
					da = da | ((i[(row-4)*width+col]&1)<<4);
					da = da | ((i[(row-5)*width+col]&1)<<5);
					da = da | ((i[(row-6)*width+col]&1)<<6);
					da = da | ((i[(row-7)*width+col]&1)<<7);
					dat[pos++] = da;
				}
			}
			// Upper left quadrant
			for(int col=23;col>=0;col=col-1) {
				for(int row=15;row>0;row=row-8) {				
					int da = 0;
					da = da | ((i[(row-0)*width+col]&1)<<0);
					da = da | ((i[(row-1)*width+col]&1)<<1);
					da = da | ((i[(row-2)*width+col]&1)<<2);
					da = da | ((i[(row-3)*width+col]&1)<<3);
					da = da | ((i[(row-4)*width+col]&1)<<4);
					da = da | ((i[(row-5)*width+col]&1)<<5);
					da = da | ((i[(row-6)*width+col]&1)<<6);
					da = da | ((i[(row-7)*width+col]&1)<<7);
					dat[pos++] = da;
				}
			}

			for(int zz=0;zz<dat.length;++zz) {
				oss.write(dat[zz]);	
				++total;
			}

		}
		
		while(total<2048) {
			oss.write(0);
			++total;
		}

	}

}
