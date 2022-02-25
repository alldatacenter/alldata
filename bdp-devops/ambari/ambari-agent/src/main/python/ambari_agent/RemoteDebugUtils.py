#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

try: import readline  # For readline input support
except: pass

import sys, signal, os, traceback, codeop, cStringIO, cPickle, tempfile

def bind_debug_signal_handlers():
  signal.signal(signal.SIGUSR1, print_threads_stack_traces) # prints process threads current stack trace to the err stream. (can be found in ambari-agent.out)
  signal.signal(signal.SIGUSR2, remote_debug) # provide a read-only python shell, which represent the process state at time of signal arrival.

def print_threads_stack_traces(sig, frame):
  print >> sys.stderr, "\n*** STACKTRACE - START ***\n"
  code = []
  for threadId, stack in sys._current_frames().items():
    code.append("\n# ThreadID: %s" % threadId)
    for filename, lineno, name, line in traceback.extract_stack(stack):
      code.append('File: "%s", line %d, in %s' % (filename,
                                                  lineno, name))
      if line:
        code.append("  %s" % (line.strip()))

  for line in code:
    print >> sys.stderr, line
  print >> sys.stderr, "\n*** STACKTRACE - END ***\n"

def pipename(pid):
  """Return name of pipe to use"""
  return os.path.join(tempfile.gettempdir(), 'debug-%d' % pid)

class NamedPipe(object):
  def __init__(self, name, end=0, mode=0666):
    """Open a pair of pipes, name.in and name.out for communication
    with another process.  One process should pass 1 for end, and the
    other 0.  Data is marshalled with pickle."""
    self.in_name, self.out_name = name +'.in',  name +'.out',
    try: os.mkfifo(self.in_name,mode)
    except OSError: pass
    try: os.mkfifo(self.out_name,mode)
    except OSError: pass
    
    # NOTE: The order the ends are opened in is important - both ends
    # of pipe 1 must be opened before the second pipe can be opened.
    if end:
      self.inp = open(self.out_name,'r')
      self.out = open(self.in_name,'w')
    else:
      self.out = open(self.out_name,'w')
      self.inp = open(self.in_name,'r')
    self._open = True

  def is_open(self):
    return not (self.inp.closed or self.out.closed)
    
  def put(self,msg):
    if self.is_open():
      data = cPickle.dumps(msg,1)
      self.out.write("%d\n" % len(data))
      self.out.write(data)
      self.out.flush()
    else:
      raise Exception("Pipe closed")
    
  def get(self):
    txt=self.inp.readline()
    if not txt: 
      self.inp.close()
    else:
      l = int(txt)
      data=self.inp.read(l)
      if len(data) < l: self.inp.close()
      return cPickle.loads(data)  # Convert back to python object.
      
  def close(self):
    self.inp.close()
    self.out.close()
    try: os.remove(self.in_name)
    except OSError: pass
    try: os.remove(self.out_name)
    except OSError: pass

  def __del__(self):
    self.close()
    
def remote_debug(sig,frame):
  """Handler to allow process to be remotely debugged."""
  def _raiseEx(ex):
    """Raise specified exception in the remote process"""
    _raiseEx.ex = ex
  _raiseEx.ex = None
  
  try:
    # Provide some useful functions.
    locs = {'_raiseEx' : _raiseEx}
    locs.update(frame.f_locals)  # Unless shadowed.
    globs = frame.f_globals
    
    pid = os.getpid()  # Use pipe name based on pid
    pipe = NamedPipe(pipename(pid))
  
    old_stdout, old_stderr = sys.stdout, sys.stderr
    txt = ''
    pipe.put("Interrupting process at following point:\n" + 
         ''.join(traceback.format_stack(frame)) + ">>> ")
    
    try:
      while pipe.is_open() and _raiseEx.ex is None:
        line = pipe.get()
        if line is None: continue # EOF
        txt += line
        try:
          code = codeop.compile_command(txt)
          if code:
            sys.stdout = cStringIO.StringIO()
            sys.stderr = sys.stdout
            exec code in globs,locs
            txt = ''
            pipe.put(sys.stdout.getvalue() + '>>> ')
          else:
            pipe.put('... ')
        except:
          txt='' # May be syntax err.
          sys.stdout = cStringIO.StringIO()
          sys.stderr = sys.stdout
          traceback.print_exc()
          pipe.put(sys.stdout.getvalue() + '>>> ')
    finally:
      sys.stdout = old_stdout # Restore redirected output.
      sys.stderr = old_stderr
      pipe.close()

  except Exception:  # Don't allow debug exceptions to propogate to real program.
    traceback.print_exc()
    
  if _raiseEx.ex is not None: raise _raiseEx.ex