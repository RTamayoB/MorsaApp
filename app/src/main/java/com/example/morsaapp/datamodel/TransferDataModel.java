package com.example.morsaapp.datamodel;

public class TransferDataModel {
    public String transferId;
    public String transferName;
    public int qtyToTransfer;

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getTransferName() {
        return transferName;
    }

    public void setTransferName(String transferName) {
        this.transferName = transferName;
    }

    public int getQtyToTransfer() {
        return qtyToTransfer;
    }

    public void setQtyToTransfer(int qtyToTransfer) {
        this.qtyToTransfer = qtyToTransfer;
    }

}
