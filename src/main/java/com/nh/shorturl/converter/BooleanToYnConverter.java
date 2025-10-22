package com.nh.shorturl.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // 이 컨버터를 모든 Boolean 타입 속성에 자동으로 적용
public class BooleanToYnConverter implements AttributeConverter<Boolean, String> {

    /**
     * Boolean 값을 DB에 저장할 'Y' 또는 'N' 문자로 변환합니다.
     * @param attribute 엔티티의 Boolean 값 (true, false)
     * @return 데이터베이스에 저장될 문자 ('Y' 또는 'N')
     */
    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        // attribute가 null이 아니고 true일 경우 'Y', 그 외에는 모두 'N'으로 처리
        return (attribute != null && attribute) ? "Y" : "N";
    }

    /**
     * DB의 'Y' 또는 'N' 문자를 Boolean 값으로 변환합니다.
     * @param dbData 데이터베이스에서 읽어온 문자 ('Y', 'N')
     * @return 엔티티의 Boolean 값 (true, false)
     */
    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        // 대소문자 구분 없이 'Y'일 경우 true, 그 외에는 모두 false로 처리
        return "Y".equalsIgnoreCase(dbData);
    }
}
