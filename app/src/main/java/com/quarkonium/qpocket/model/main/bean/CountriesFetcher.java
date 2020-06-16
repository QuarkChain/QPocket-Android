package com.quarkonium.qpocket.model.main.bean;


import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.quarkonium.qpocket.api.Constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CountriesFetcher {
    private static CountryList mCountries;

    /**
     * Import CountryList from RAW resource
     *
     * @return CountryList
     */
    public static CountryList getCountries() {
        if (mCountries != null) {
            return mCountries;
        }
        mCountries = new CountryList();
        List<String> notSupport = Arrays.asList(Constant.NOT_SUPPORT_PHONE_COUNTRY);
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Set<String> countries = phoneUtil.getSupportedRegions();
        for (String country : countries) {
            if (notSupport.contains(country)) {
                continue;
            }

            int code = phoneUtil.getCountryCodeForRegion(country);
            Country temp = new Country(country, country.toLowerCase(), code);
            mCountries.add(temp);
        }
        Collections.sort(mCountries);
        return mCountries;
    }


    public static class CountryList extends ArrayList<Country> {
        /**
         * Fetch item index on the list by iso
         *
         * @param iso Country's iso2
         * @return index of the item in the list
         */
        public int indexOfIso(String iso) {
            for (int i = 0; i < this.size(); i++) {
                if (this.get(i).getIso().toUpperCase().equals(iso.toUpperCase())) {
                    return i;
                }
            }
            return -1;
        }

        public Country findOfIso(String iso) {
            for (int i = 0; i < this.size(); i++) {
                if (this.get(i).getIso().toUpperCase().equals(iso.toUpperCase())) {
                    return get(i);
                }
            }
            return null;
        }

        public Country findOfDialCode(String dialCode) {
            int code = -1;
            try {
                code = Integer.valueOf(dialCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (code == -1) {
                return null;
            }

            for (int i = 0; i < this.size(); i++) {
                if (this.get(i).getDialCode() == code) {
                    return get(i);
                }
            }
            return null;
        }

        /**
         * Fetch item index on the list by dial coder
         *
         * @param dialCode Country's dial code prefix
         * @return index of the item in the list
         */
        @SuppressWarnings("unused")
        public int indexOfDialCode(int dialCode) {
            for (int i = 0; i < this.size(); i++) {
                if (this.get(i).getDialCode() == dialCode) {
                    return i;
                }
            }
            return -1;
        }
    }
}
