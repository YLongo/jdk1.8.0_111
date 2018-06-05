package org.omg.PortableInterceptor;


/**
* org/omg/PortableInterceptor/ObjectReferenceTemplateHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from c:/re/workspace/8-2-build-windows-amd64-cygwin/jdk8u111/7883/corba/src/share/classes/org/omg/PortableInterceptor/Interceptors.idl
* Thursday, September 22, 2016 7:25:19 PM PDT
*/


/** The object reference template.  An instance of this must
   * exist for each object adapter created in an ORB.  The server_id,
   * orb_id, and adapter_name attributes uniquely identify this template
   * within the scope of an IMR.  Note that adapter_id is similarly unique
   * within the same scope, but it is opaque, and less useful in many
   * cases.
   */
abstract public class ObjectReferenceTemplateHelper
{
  private static String  _id = "IDL:omg.org/PortableInterceptor/ObjectReferenceTemplate:1.0";


  public static void insert (org.omg.CORBA.Any a, org.omg.PortableInterceptor.ObjectReferenceTemplate that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.omg.PortableInterceptor.ObjectReferenceTemplate extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (org.omg.CORBA.TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          org.omg.CORBA.ValueMember[] _members0 = new org.omg.CORBA.ValueMember[0];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          __typeCode = org.omg.CORBA.ORB.init ().create_value_tc (_id, "ObjectReferenceTemplate", org.omg.CORBA.VM_ABSTRACT.value, null, _members0);
          __active = false;
        }
      }
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.omg.PortableInterceptor.ObjectReferenceTemplate read (org.omg.CORBA.portable.InputStream istream)
  {
    return (org.omg.PortableInterceptor.ObjectReferenceTemplate)((org.omg.CORBA_2_3.portable.InputStream) istream).read_value (id ());
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.omg.PortableInterceptor.ObjectReferenceTemplate value)
  {
    ((org.omg.CORBA_2_3.portable.OutputStream) ostream).write_value (value, id ());
  }


}
