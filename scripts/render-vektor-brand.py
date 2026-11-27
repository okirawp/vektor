#!/usr/bin/env python3
"""Render the original SVG brand assets with host librsvg/Cairo; no app dependency."""
import ctypes as C,ctypes.util
from pathlib import Path
root=Path(__file__).resolve().parents[1]
rsvg=C.CDLL(ctypes.util.find_library('rsvg-2'));cairo=C.CDLL(ctypes.util.find_library('cairo'));gobj=C.CDLL(ctypes.util.find_library('gobject-2.0'))
rsvg.rsvg_handle_new_from_file.argtypes=[C.c_char_p,C.POINTER(C.c_void_p)];rsvg.rsvg_handle_new_from_file.restype=C.c_void_p
rsvg.rsvg_handle_render_cairo.argtypes=[C.c_void_p,C.c_void_p];rsvg.rsvg_handle_render_cairo.restype=C.c_int
cairo.cairo_image_surface_create.argtypes=[C.c_int,C.c_int,C.c_int];cairo.cairo_image_surface_create.restype=C.c_void_p
cairo.cairo_create.argtypes=[C.c_void_p];cairo.cairo_create.restype=C.c_void_p
cairo.cairo_surface_write_to_png.argtypes=[C.c_void_p,C.c_char_p];cairo.cairo_surface_write_to_png.restype=C.c_int
cairo.cairo_destroy.argtypes=[C.c_void_p];cairo.cairo_surface_destroy.argtypes=[C.c_void_p];gobj.g_object_unref.argtypes=[C.c_void_p]
for source,target,w,h in [('icon.svg','icon-512.png',512,512),('feature.svg','feature-1024x500.png',1024,500)]:
 error=C.c_void_p();handle=rsvg.rsvg_handle_new_from_file(str(root/'assets/vektor'/source).encode(),C.byref(error));assert handle and not error.value
 surface=cairo.cairo_image_surface_create(0,w,h);context=cairo.cairo_create(surface)
 assert rsvg.rsvg_handle_render_cairo(handle,context)
 assert cairo.cairo_surface_write_to_png(surface,str(root/'assets/vektor'/target).encode())==0
 cairo.cairo_destroy(context);cairo.cairo_surface_destroy(surface);gobj.g_object_unref(handle)
 print('Rendered '+target)
