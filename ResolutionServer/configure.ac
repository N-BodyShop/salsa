#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_DEFUN([AX_PYTHON],
[AC_MSG_CHECKING(for python build information)
AC_MSG_RESULT([])
for python in python2.4 python2.3 python2.2 python2.1 python; do
AC_CHECK_PROGS(PYTHON_BIN, [$python])
ax_python_bin=$PYTHON_BIN
if test x$ax_python_bin != x; then
   LDFLAGS="-L/usr/lib/$python/config -l$python"
   AC_CHECK_LIB($ax_python_bin, main, ax_python_lib=$python, ax_python_lib=no)
   AC_CHECK_HEADER([$ax_python_bin/Python.h],
   [[ax_python_header=`locate $ax_python_bin/Python.h | sed -e s,/Python.h,,`]],
   ax_python_header=no)
   if test $ax_python_lib != no; then
     if test $ax_python_header != no; then
	ax_python_lib="-L/usr/lib/$python/config -l$python"
       break;
     fi
   fi
fi
done
if test x$ax_python_bin = x; then
   ax_python_bin=no
fi
if test x$ax_python_header = x; then
   ax_python_header=no
fi
if test x"$ax_python_lib" = x; then
   ax_python_lib=no
fi

AC_MSG_RESULT([  results of the Python check:])
AC_MSG_RESULT([    Binary:      $ax_python_bin])
AC_MSG_RESULT([    Library:     $ax_python_lib])
AC_MSG_RESULT([    Include Dir: $ax_python_header])

if test x$ax_python_header != xno; then
  PYTHON_INCLUDE_DIR=$ax_python_header
  AC_SUBST(PYTHON_INCLUDE_DIR)
fi
if test x"$ax_python_lib" != xno; then
  PYTHON_LIB="$ax_python_lib"
  AC_SUBST(PYTHON_LIB)
fi
])dnl

AC_PREREQ(2.57)
AC_INIT(FULL-PACKAGE-NAME, VERSION, BUG-REPORT-ADDRESS)
AC_CONFIG_SRCDIR([HighLevelPython.cpp])
AC_CONFIG_HEADER([config.h])

# Checks for programs.
AC_PROG_CXX
AC_PROG_CC
AC_PATH_PROG([CHARMC], [charmc], [../charm/bin], [$PATH:../charm/bin])

# Checks for libraries.
AC_CHECK_LIB([popt], [poptGetContext])
AC_CHECK_LIB([pthread], [pthread_attr_destroy])
AX_PYTHON
# FIXME: Replace `main' with a function in `-lTipsy':
AC_CHECK_LIB([Tipsy], [main])

# find boost
ax_boost_header=no
for boost in $PWD/../boost $PWD/../boost_1_31_0 ; do
   if test -f $boost/boost/shared_ptr.hpp ; then
       ax_boost_header=yes
       BOOST_PATH=$boost
       break;
   fi
done
if test $ax_boost_header != yes; then
   echo Boost headers not found
fi
AC_MSG_RESULT([  results of the Boost check:])
AC_MSG_RESULT([    Directory:      $boost])
AC_SUBST(BOOST_PATH)

# find xerces-c
ax_xercesc=no
for xercesc in $PWD/../xercesc $PWD/../xerces-c-src2_4_0 $PWD/../xerces-c-src_2_5_0 $PWD/../xerces-c-src_2_6_0 ; do
   if test -f $xercesc/include/xercesc/sax2/DefaultHandler.hpp ; then
       ax_xercesc_header=yes
       XERCESCROOT=$xercesc
       break;
   fi
done
if test $ax_xercesc_header != yes; then
   echo xercesc headers not found
fi
AC_MSG_RESULT([  results of the xercesc check:])
AC_MSG_RESULT([    Directory:      $xercesc])
AC_SUBST(XERCESCROOT)

# Checks for header files.
AC_CHECK_HEADERS([stdint.h])

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_C_CONST
AC_C_INLINE
AC_TYPE_SIZE_T

# Checks for library functions.
AC_HEADER_STDC
AC_CHECK_FUNCS([floor memset sqrt])

ln -s $PYTHON_INCLUDE_DIR python

AC_CONFIG_SUBDIRS(../structures)
AC_CONFIG_FILES([Makefile])
AC_OUTPUT