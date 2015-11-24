//PixelDrawing.cpp

void drawLine(byte* image, const int width, const int height, int x0, int y0, int x1, int y1, const int color) {
	int dx = 1;
	int a = x1 - x0;
	if(a < 0) {
		dx = -1;
		a = -a;
	}
	
	int dy = 1;
	int b = y1 - y0;
	if(b < 0) {
		dy = -1;
		b = -b;
	}
	
	int two_a = 2 * a, two_b = 2 * b;
	int xcrit = two_a - b;
	int eps = 0;
	
	for(;;) {
		if(x0 > 0 && x0 < width && y0 > 0 && y0 < height && image[x0 + width * y0] < color)
			image[x0 + width * y0] = color;
		if(x0 == x1 && y0 == y1)
			break;
		if(eps <= xcrit) {
			x0 += dx;
			eps += two_b;
		}
		if(eps >= a || a <= b) {
			y0 += dy;
			eps -= two_a;
		}
	}
}

/// Draw eight points corresponding to points on a circle
void drawCirclePoints(byte* image, const int width, const int height, const int xCenter, const int yCenter, const int x, const int y, const int color) {
	int xpix, ypix;
	
	xpix = xCenter + x;
	ypix = yCenter + y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter - x;
	ypix = yCenter + y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter + x;
	ypix = yCenter - y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter - x;
	ypix = yCenter - y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	if(x == y) //the mirror points are the same in this case, don't bother
		return;
	xpix = xCenter + y;
	ypix = yCenter + x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter - y;
	ypix = yCenter + x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter + y;
	ypix = yCenter - x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter - y;
	ypix = yCenter - x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
}

/// Draw a circle in pixels, using the Bresenham Algorithm
void drawCircle(byte* image, const int width, const int height, const int x0, const int y0, const int radius, const int color) {
	int x = 0;
	int y = radius;
	int p = 1 - radius;
	
	drawCirclePoints(image, width, height, x0, y0, x, y, color);
	while(x < y) {
		++x;
		if(p < 0)
			p += 2 * x + 1;
		else {
			--y;
			p += 2 * (x - y) + 1;
		}
		drawCirclePoints(image, width, height, x0, y0, x, y, color);
	}
}

void drawDisk(byte* image, const int width, const int height, const int x0, const int y0, const int radius, const int color) {
	int x = 0;
	int y = radius;
	int p = 1 - radius;
	
	drawCirclePoints(image, width, height, x0, y0, x, y, color);
	if(y > 0)
		drawLine(image, width, height, x0 - y + 1, y0 - x, x0 + y - 1, y0 - x, color);
	while(x < y) {
		++x;
		if(p < 0)
			p += 2 * x + 1;
		else {
			--y;
			p += 2 * (x - y) + 1;
		}
		drawCirclePoints(image, width, height, x0, y0, x, y, color);
		if(y > 0) {
			drawLine(image, width, height, x0 - y + 1, y0 - x, x0 + y - 1, y0 - x, color);
			drawLine(image, width, height, x0 - y + 1, y0 + x, x0 + y - 1, y0 + x, color);
		}
	}
}

SplineInterpolator<double> projectedKernel;

void initializeProjectedKernel(const int N) {
	vector<double> xs(N, 2.0 / N);
	xs[0] = 0;
	partial_sum(xs.begin(), xs.end(), xs.begin());
	
	vector<double> pts;
	pts.reserve(N);
	
	SplineKernel kernel;
        transform(xs.begin(), xs.end(), back_inserter(pts), bind(&SplineKernel::evaluateProjection, boost::ref(kernel), _1, 1.0));
	
	projectedKernel = SplineInterpolator<double>(xs.begin(), xs.end(), pts.begin(), pts.end(), 0, 0);
}

template <typename T>
inline T clamp(T val) {
	if(val < 0)
		return 0;
	else if(val > 1)
		return 1;
	else
		return val;
}

inline void splatParticle(byte* image, const int width, const int height, const float x, const float y, const float m, const float h, const float delta, const byte endColor, const double minAmount, const double maxAmount) {
	int partxpix = static_cast<int>(width * (x + 1) / 2);
	int partypix = static_cast<int>(height * (1 - y) / 2);
	int pixel = partxpix + partypix * width;
	//is the particle contained in one pixel?
	if(2 * h <= delta) {
		if(pixel >= 0 && pixel < width * height) {
			double amount = clamp((m - minAmount) / (maxAmount - minAmount));
			image[pixel] = static_cast<byte>(max(1,min(endColor, 
				image[pixel] + endColor * amount
			)));
		}
	} else {
		unsigned int hint = static_cast<unsigned int>(ceil(h / delta));
		unsigned int minxpix = max(0, partxpix - 2 * hint);
		unsigned int maxxpix = min(width - 1, partxpix + 2 * hint);
		unsigned int minypix = max(0, partypix - 2 * hint);
		unsigned int maxypix = min(height - 1, partypix + 2 * hint);
		// float dAOverhsq = delta * delta / h / h;
		for(unsigned int ypix = minypix; ypix <= maxypix; ++ypix) {
			for(unsigned int xpix = minxpix; xpix <= maxxpix; ++xpix) {
				float r = delta * sqrt(static_cast<double>((ypix - partypix) * (ypix - partypix) + (xpix - partxpix) * (xpix - partxpix)));
				if(r < 2 * h) {
					//interpolated table is for h=1, so include scaling factor 1/h^2 in dAOverhsq
					pixel = xpix + ypix * width;
					if(pixel < 0 || pixel >= width * height) {
						cerr << "Errant pixel: " << pixel << endl;
						cerr << "x: " << x << " y: " << y << endl;
						cerr << "particle x: " << partxpix << " y: " << partypix << endl;
						cerr << "xpix range: " << minxpix << " " << maxxpix << endl;
						cerr << "ypix range: " << minypix << " " << maxypix << endl;
						cerr << "xpix: " << xpix << " ypix: " << ypix << endl;
						cerr << "hint: " << hint << endl;
						cerr << "w: " << width << " h: " << height << endl;
					}
					//byte value = static_cast<byte>(m * projectedKernel(r / h) * dAOverhsq);
					//amount > 0
					double amount = m * projectedKernel(r / h);// * dAOverhsq;
					//amount \in [0,1]
					amount = clamp((amount - minAmount) / (maxAmount - minAmount));
					//value \in {0, 1, ... , endColor}
					image[pixel] = static_cast<byte>(max(1,min(endColor, 
						image[pixel] + endColor * amount
					)));
				}
			}
		}
	}
}
