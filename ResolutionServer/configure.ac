#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_DEFUN([AX_PYTHON],
[AC_MSG_CHECKING(for python build information)
AC_MSG_RESULT([])
for python in python2.4 python2.3 python2.2 python2.1 python; do
   AC_CHECK_HEADER([$python/Python.h],
   	[[ax_python_header=/usr/include/$python]],
   	ax_python_header=no)
   LDFLAGS="-L/usr/lib/$python/config -l$python"
   AC_CHECK_LIB($python, main, ax_python_lib=$python, ax_python_lib=no)
   if test $ax_python_lib != no; then
     if test $ax_python_header != no; then
	ax_python_lib="-L/usr/lib/$python/config -l$python"
       break;
     fi
   fi
done
if test x$ax_python_header = x; then
   ax_python_header=no
fi
if test x"$ax_python_lib" = x; then
   ax_python_lib=no
fi

AC_MSG_RESULT([  results of the Python check:])
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
AC_PATH_PROG([CHARMC], [charmc], [$CHARM_DIR/bin/charmc], [$PATH:$CHARM_DIR/bin])
AC_PROG_CXX ($CHARMC)
AC_PROG_CC ($CHARMC)

# Checks for libraries.
AC_CHECK_LIB([popt], [poptGetContext])
AC_CHECK_LIB([pthread], [pthread_attr_destroy])
AC_CHECK_LIB([rt], [sem_init])
AX_PYTHON
# FIXME: Replace `main' with a function in `-lTipsy':
AC_CHECK_LIB([Tipsy], [main])

# find boost
ax_boost_header=no
for boost in /usr/include $PWD/../boost $PWD/../boost_1_31_0 $PWD/../boost_1_32_0 $PWD/../boost_1_33_0 ; do
   if test -f $boost/boost/shared_ptr.hpp ; then
       ax_boost_header=yes
       BOOST_PATH=$boost
       break;
   fi
done
if test $ax_boost_header != yes; then
   echo Boost headers not found
   echo Download boost from www.boost.org and unpack in $PWD/..
   exit
fi
AC_MSG_RESULT([  results of the Boost check:])
AC_MSG_RESULT([    Directory:      $boost])
AC_SUBST(BOOST_PATH)

# find xerces-c
ax_xercesc_header=no
if test "x$XERCESCROOT" != "x"; then
  if test -f $XERCESCROOT/include/xercesc/sax2/DefaultHandler.hpp ; then
       ax_xercesc_header=yes
   fi
else
for xercesc in $PWD/../xercesc $PWD/../xerces-c-src2_4_0 $PWD/../xerces-c-src_2_5_0 $PWD/../xerces-c-src_2_6_0 $PWD/../xerces-c-src_2_7_0 ; do
   if test -f $xercesc/include/xercesc/sax2/DefaultHandler.hpp ; then
       ax_xercesc_header=yes
       XERCESCROOT=$xercesc
       break;
   fi
done
fi
if test $ax_xercesc_header != yes; then
   echo xercesc headers not found
   echo Download boost from www.apache.org and install in $PWD/..
   exit
fi
AC_MSG_RESULT([  results of the xercesc check:])
AC_MSG_RESULT([    Directory:      $xercesc])
AC_SUBST(XERCESCROOT)

# Checks for header files.
AC_CHECK_HEADERS([stdint.h endian.h machine/endian.h])

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_C_CONST
AC_C_INLINE
AC_TYPE_SIZE_T
AC_CHECK_TYPE(u_int64_t, uint64_t)

# Checks for library functions.
AC_HEADER_STDC
AC_CHECK_FUNCS([floor memset sqrt xdr_hyper])

rm -f python
ln -s $PYTHON_INCLUDE_DIR python

AC_CONFIG_SUBDIRS(../structures)
AC_CONFIG_FILES([Makefile])
AC_OUTPUT
