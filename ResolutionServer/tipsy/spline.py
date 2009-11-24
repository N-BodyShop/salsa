import traceback

def s_circe(x, t) :
    try :
        print circe(x, t)
    except:
        print traceback.format_exc()

def s_tridi(a, b, c, f, x, n) :
    try :
        print tridi(a, b, c, f, x, n)
    except:
        print traceback.format_exc()

def s_splinit(x, y, k, n, q2b, q2e) :
    try :
        print splinit(x, y, k, n, q2b, q2e)
    except:
        print traceback.format_exc()

def s_spf(dx, x, y, k, m, key) :
    try :
        print spf(dx, x, y, k, m, key)
    except:
        print traceback.format_exc()

def s_spft(z, x, y, k, n, key) :
    try :
        print spft(z, x, y, k, n, key)
    except:
        print traceback.format_exc()

def circe(x, t) :
    """ returns index i of first element of t >= x.
    values in t must be sorted. will return extrema if
    x is outside the range of t. """
    lo = 0
    hi = len(t) - 1
    if x > t[lo] and x < t[hi] :
        while hi - lo > 1 :
            mid = (lo + hi) / 2
            if x <= t[mid] :
                hi = mid
            else :
                lo = mid
        return hi
    if x <= t[lo] :
        return lo
    if x >= t[hi] :
        return hi

def tridi(a, b, c, f, x, n) :
    """ solves tridiagonal linear system.
        diag elts mii=ai, subdiag mii-1=bi, superdiag mii+1=ci.
        it is understood that b0 and cn-1 are zero, but are not referenced.
        f is rhs, x soln, may be same array.
        """
    for i in range(1, n) :
        b[i] /= a[i-1]
        a[i] -= b[i] * c[i-1]
    for i in range(1, n) :
        f[i] -= b[i] * f[i-1]
    x[n-1] = f[n-1] / a[n-1]
    for i in range(n-2, -1, -1) :
        x[i] = (f[i] - (c[i] * x[i+1])) / a[i]

def splinit(x, y, k, n, q2b, q2e) :
    """ sets up spline derivative array k for a
        given x and y array of length n POINTS, n-1 intervals, for
        given estimates for the second derivatives at the endpoints,
        q2b and q2e; "natural" boundary conditions for q2b=q2e=0.
        """
    a = [None] * n
    b = [None] * n
    c = [None] * n
    f = [None] * n
    hio = 0.
    dio = 0.
    for i in range(n) :
        ip = i + 1
        hip = x[ip] - x[i] if ip < n else 0
        dip = (y[i + 1] - y[i]) / hip if ip < n else 0
        b[i] = hip if ip < n else hio
        a[i] = 2. * (hip + hio)
        c[i] = hio if i > 0 else hip
        f[i] = 3. * (hip * dio + hio * dip)
        if i == 0 :
            f[0] = 3. * hip * dip - hip * hip * q2b * 0.5
        elif i == n-1 :
            f[n-1] = 3. * hio * dio + hio * hio * q2e * 0.5
        dio = dip
        hio = hip
    tridi(a, b, c, f, k, n)

def spf(dx, x, y, k, m, key) :
    """ general spline evaluator; xyk are the x, y, and
        derivative arrays, dx the argument = (x-xi-1), m the index of
        the next GREATER abscissa. If key!=0, routine evaluates derivative.
        """
    h = x[m] - x[m-1]
    d = (y[m] - y[m-1]) / h
    t = dx / h
    tm = 1. - t
    a = (k[m-1] - d) * tm
    b = (k[m] - d) * t
    if not(key) :
        val = t * y[m] + tm * y[m-1] + h*t*tm*(a-b)
    else :
        val = d + a*(1.-3*t) - b*(2.-3*t)
    return val

def spft(z, x, y, k, n, key) :
    """ spline evaluator for general monotonic
        (increasing) abscissa tables.
        """
    m = circe(z, x)
    dx = z - x[m-1]
    return spf(dx, x, y, k, m, key)

