package org.drools.gorm

import java.sql.Blob
import java.sql.SQLException

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
		byte[] buf = new byte[4000]
		InputStream is = fromBlob.getBinaryStream()
		try {
			while (true) {
				int dataSize = is.read(buf)

				if (dataSize == -1)	break
				baos.write(buf, 0, dataSize)
			}
		} finally {
			is.close()
		}
		return baos.toByteArray()
	}
	
}
