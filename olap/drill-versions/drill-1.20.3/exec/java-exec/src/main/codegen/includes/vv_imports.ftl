<#--

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

-->
import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkArgument;
import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkState;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.ObjectArrays;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.ObjectArrays;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import io.netty.buffer.*;

import org.apache.commons.lang3.ArrayUtils;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.expr.fn.impl.StringFunctionUtil;
import org.apache.drill.exec.memory.*;
import org.apache.drill.exec.proto.SchemaDefProtos;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.*;
import org.apache.drill.exec.vector.*;
import org.apache.drill.common.exceptions.*;
import org.apache.drill.exec.exception.*;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.types.TypeProtos.*;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.vector.complex.*;
import org.apache.drill.exec.vector.complex.reader.*;
import org.apache.drill.exec.vector.complex.impl.*;
import org.apache.drill.exec.vector.complex.writer.*;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter;
import org.apache.drill.exec.util.JsonStringArrayList;

import org.apache.drill.exec.exception.OutOfMemoryException;

import com.sun.codemodel.JType;
import com.sun.codemodel.JCodeModel;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.Random;
import java.util.List;

import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.joda.time.DateTime;
import org.joda.time.Period;

import org.apache.drill.exec.util.Text;

import org.apache.drill.exec.vector.accessor.sql.TimePrintMillis;
import javax.inject.Inject;





