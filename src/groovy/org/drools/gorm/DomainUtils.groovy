package org.drools.gorm

import java.sql.Blob
import java.sql.SQLException

import org.drools.gorm.session.ProcessInstanceInfoDomain;


class DomainUtils {
	static blobToByteArray(Blob fromBlob) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		try {
			return blobToByteArrayImpl(fromBlob, baos)
		} catch (SQLException e) {
			throw new RuntimeException(e)
		} catch (IOException e) {
			throw new RuntimeException(e)
		} finally {
			baos.close()
		}
	}

	private static byte[] blobToByteArrayImpl(Blob fromBlob, ByteArrayOutputStream baos) {
		InputStream is = null
		try {
			byte[] buf = new byte[4000]
			is = fromBlob.getBinaryStream()
			int dataSize
			while ((dataSize = is.read(buf)) != -1) {
				baos.write(buf, 0, dataSize)
			}
			return baos.toByteArray()
		} finally {
			if (is != null) {
				is.close()
			}
		}
	}
	
	public static List<Long> ProcessInstancesWaitingForEvent(String type) {
		def query = """\
			select 
			    pii.processInstanceId
			from 
			    ProcessInstanceInfo pii
			where
			    ? in elements(pii.eventTypes)
			"""
		return ProcessInstanceInfoDomain.findAll("query", [type]);
	}
	
}
