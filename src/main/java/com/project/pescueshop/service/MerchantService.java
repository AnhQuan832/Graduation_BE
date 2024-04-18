package com.project.pescueshop.service;

import com.project.pescueshop.model.dto.CreateMerchantRequest;
import com.project.pescueshop.model.dto.MerchantDTO;
import com.project.pescueshop.model.entity.Merchant;
import com.project.pescueshop.model.entity.User;
import com.project.pescueshop.model.exception.FriendlyException;
import com.project.pescueshop.repository.dao.MerchantDAO;
import com.project.pescueshop.util.constant.EnumResponseCode;
import com.project.pescueshop.util.constant.EnumRoleId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
public class MerchantService extends BaseService {
    private final MerchantDAO merchantDAO;
    private final ThreadService threadService;
    private final UserService userService;

    public MerchantDTO toDTO(Merchant merchant){
        return MerchantDTO.builder()
                .merchantId(merchant.getMerchantId())
                .merchantName(merchant.getMerchantName())
                .merchantAvatar(merchant.getMerchantAvatar())
                .merchantCover(merchant.getMerchantCover())
                .merchantDescription(merchant.getMerchantDescription())
                .createdDate(merchant.getCreatedDate())
                .cityName(merchant.getCityName())
                .districtName(merchant.getDistrictName())
                .wardName(merchant.getWardName())
                .cityName(merchant.getCityCode())
                .districtCode(merchant.getDistrictCode())
                .wardCode(merchant.getWardCode())
                .phoneNumber(merchant.getPhoneNumber())
                .userId(merchant.getUserId())
                .noProduct(merchant.getNoProduct())
                .relatedDocuments(merchant.getRelatedDocuments() == null ? new ArrayList<>() : merchant.getRelatedDocuments())
                .isSuspended(merchant.getIsSuspended())
                .isLiveable(merchant.getIsLiveable())
                .build();
    }

    public MerchantDTO createNewMerchantRequest(User user, CreateMerchantRequest request, MultipartFile[] relatedDocumentsFile, MultipartFile avatarFile, MultipartFile coverImageFile) {
        Merchant merchant = Merchant.builder()
                .merchantName(request.getMerchantName())
                .merchantDescription(request.getMerchantDescription())
                .createdDate(new Date())
                .cityName(request.getCityName())
                .districtName(request.getDistrictName())
                .wardName(request.getWardName())
                .cityCode(request.getCityCode())
                .districtCode(request.getDistrictCode())
                .wardCode(request.getWardCode())
                .phoneNumber(request.getPhoneNumber())
                .userId(user.getUserId())
                .noProduct(0)
                .isSuspended(false)
                .isLiveable(true)
                .build();

        merchantDAO.saveAndFlushMerchant(merchant);

        threadService.uploadMerchantFiles(merchant, List.of(relatedDocumentsFile), avatarFile, coverImageFile);

        merchantDAO.saveAndFlushMerchant(merchant);

        CompletableFuture.runAsync(() -> {
            try {
                userService.addUserRole(user.getUserId(), EnumRoleId.MERCHANT);
            } catch (FriendlyException e) {
                throw new RuntimeException(e);
            }
        });

        return toDTO(merchant);
    }

    public void suspendMerchant(String merchantId) throws FriendlyException {
        Merchant merchant = merchantDAO.getMerchantById(merchantId);

        if (merchant == null) {
            throw new FriendlyException(EnumResponseCode.MERCHANT_NOT_FOUND);
        }

        merchant.setIsSuspended(true);
        merchantDAO.saveAndFlushMerchant(merchant);
    }

    public void unsuspendMerchant(String merchantId) throws FriendlyException {
        Merchant merchant = merchantDAO.getMerchantById(merchantId);

        if (merchant == null) {
            throw new FriendlyException(EnumResponseCode.MERCHANT_NOT_FOUND);
        }

        merchant.setIsSuspended(false);
        merchantDAO.saveAndFlushMerchant(merchant);
    }

    public Merchant getMerchantById(String merchantId) {
        return merchantDAO.getMerchantById(merchantId);
    }

    public Merchant getMerchantByUserId(String userId) {
        return merchantDAO.getMerchantByUserId(userId);
    }

    public MerchantDTO getMerchantInfo(String merchantId) throws FriendlyException {
        Merchant merchant = null;

        if (merchantId != null) {
            merchant = getMerchantById(merchantId);
        }
        else {
            User user = AuthenticationService.getCurrentLoggedInUser();
            merchant = merchantDAO.getMerchantByUserId(user.getUserId());
        }

        if (merchant == null) {
            throw new FriendlyException(EnumResponseCode.MERCHANT_NOT_FOUND);
        }

        if (merchant.getIsSuspended()){
            throw new FriendlyException(EnumResponseCode.MERCHANT_SUSPENDED);
        }

        return toDTO(merchant);
    }
}
