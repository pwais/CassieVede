#!/usr/bin/env python
import capnp
capnp.remove_import_hook()
import sys
print sys.path
meow = capnp.load('/Users/pwais/Documents/CassieVede/src/main/resources/CVImage.capnp')#imports=['/Users/pwais/Documents/CassieVede/deps/capnproto/include/', '/Users/pwais/Documents/CassieVede/deps/capnproto-java/compiler/src/main/schema/'])
